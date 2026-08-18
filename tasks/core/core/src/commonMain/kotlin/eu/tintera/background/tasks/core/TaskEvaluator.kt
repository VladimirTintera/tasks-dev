package eu.tintera.background.tasks.core

import eu.tintera.background.tasks.ForegroundInfo
import eu.tintera.background.tasks.ParentData
import eu.tintera.background.tasks.TaskResult
import eu.tintera.background.tasks.core.data.TaskEvaluatorRepository
import eu.tintera.background.tasks.core.migrations.TaskMigrator
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException
import kotlin.uuid.Uuid

enum class TaskEvaluatorResult {
    SUCCESS,
    FAILURE,
    RETRY
}


interface TaskEvaluator {
    suspend fun handle(
        id: Uuid,
        onForegroundInfo: suspend (ForegroundInfo) -> Boolean
    ): TaskEvaluatorResult
}

class TaskEvaluatorImpl(
    private val registryResolver: RegistryResolver,
    private val taskMigrator: TaskMigrator,
    private val taskScopeFactory: TaskScopeFactory,
    private val applicationScope: ApplicationScope,
    private val dispatchers: AppDispatchers,
    private val tagMapper: TagMapper,
    private val repository: TaskEvaluatorRepository,
    private val taskResultHandler: TaskResultHandler,
    private val log: CompositeTasksLogger
) : TaskEvaluator {

    override suspend fun handle(
        id: Uuid,
        onForegroundInfo: suspend (ForegroundInfo) -> Boolean
    ): TaskEvaluatorResult {

        val task = repository.executableTask(id) ?: return TaskEvaluatorResult.FAILURE.also {
            log.warning(TAG) { "Task $id not found" }
        }

        // Failed, ne Retry — a to schválně. Handler, který aplikace přestala používat (nahradila
        // ho jiným nebo ho opustila), zmizí z registrace, ale jeho dřív naplánované tasky ve frontě
        // zůstanou. Ty musí definitivně selhat, jinak by se donekonečna probouzely.
        //
        // Závod se startem aplikace (systém spustí task dřív, než konzument postaví svůj Koin)
        // řeší warmup okno v registru — do té doby resolve počká. Když ani ono nestačí, je to
        // konfigurace: TaskManagerConfiguration.registryWarmupTimeout.
        val registration = registryResolver.resolve<Any, Any, Any>(
            identifier = task.identifier
        ) ?: return handleResult(
            TaskEvaluationResult.Failed(
                id = id,
                repeatInterval = null
            )
        ).also {
            log.error(TAG) {
                "No registration found for task $id (identifier '${task.identifier}') — failing it. " +
                    "Buď jde o task naplánovaný handlerem, který už aplikace neregistruje, nebo se " +
                    "identifier v registraci neshoduje s tím v TaskRequest. Pokud aplikace startuje " +
                    "pomalu, zvaž zvýšení TaskManagerConfiguration.registryWarmupTimeout."
            }
        }

        val migrationResult = runCatching {
            taskMigrator.migrate(data = task, registration = registration)
        }.getOrElse { e ->
            // Chybějící migrační cesta nebo downgrade (task uložila novější verze aplikace, po
            // rollbacku ho čte starší). Vyhodit to ven znamená shodit workera — task raději
            // ukončíme jako Failed, ať se fronta nezasekne.
            log.error(TAG, e) {
                "Migration failed for task $id (identifier '${task.identifier}', version ${task.version} " +
                    "→ ${registration.currentVersion})"
            }
            return handleResult(TaskEvaluationResult.Failed(id = id, repeatInterval = null))
        }?.also {
            repository.upgradeData(
                id = id,
                input = it.input?.let { input ->
                    registration.inputSerializer.encodeToBytes(input)
                } ?: task.inputData,
                output = it.output?.let { output ->
                    registration.outputSerializer.encodeToBytes(output)
                } ?: task.outputData,
                progress = it.progress?.let { progress ->
                    registration.progressSerializer.encodeToBytes(progress)
                } ?: task.progressData,
                version = it.version
            )
        }

        val typedInput = migrationResult?.input ?: task.inputData?.let {
            registration.inputSerializer.decodeFromBytes(it)
        } ?: return handleResult(
            TaskEvaluationResult.Failed(
                id = id,
                repeatInterval = null
            )
        )

        val parents = repository.parentsDataFor(id).mapNotNull { parentEntity ->
            registryResolver.resolve<Any, Any, Any>(parentEntity.identifier)?.let { parentRegistration ->
                val migratedData = taskMigrator.migrate(data = parentEntity, registration = parentRegistration)
                ParentData(
                    id = parentEntity.id,
                    identifier = parentEntity.identifier,
                    data = parentEntity.outputData?.let {
                        migratedData?.output ?: parentRegistration.outputSerializer.decodeFromBytes(it)
                    },
                    finishedAt = parentEntity.finishedAt,
                    handlerType = parentRegistration.type
                )
            }
        }

        val result = taskScopeFactory.createForTask(
            taskId = id,
            data = typedInput,
            retryCount = task.runAttemptCount - 1,
            parentData = parents,
            onForegroundInfoProvided = onForegroundInfo,
            progressSerializer = registration.progressSerializer,
            scope = applicationScope + dispatchers.default,
            tags = tagMapper.parse(tags = task.tags).toSet(),
            saveDispatcher = dispatchers.io
        ).use { scope ->
            try {
                with(registration.factory()) {
                    scope.run()
                }.also {
                    scope.flushProgress()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log.error(TAG, e) { "Task $id (identifier '${task.identifier}') threw" }
                TaskResult.Failure
            }
        }

        return handleResult(
            when (result) {
                TaskResult.Failure -> TaskEvaluationResult.Failed(
                    id = id,
                    repeatInterval = task.repeatInterval
                )

                TaskResult.Retry -> TaskEvaluationResult.Retry(
                    id = id,
                    backoffCriteria = task.backoffCriteria,
                    retryCount = task.runAttemptCount - 1
                )

                is TaskResult.Success -> TaskEvaluationResult.Success(
                    id = id,
                    registration = registration,
                    repeatInterval = task.repeatInterval,
                    outputData = result.outputData,
                )
            }
        )
    }

    private suspend fun handleResult(
        result: TaskEvaluationResult
    ): TaskEvaluatorResult {

        withContext(NonCancellable) {
            taskResultHandler.handleResult(result)
        }

        return when (result) {
            is TaskEvaluationResult.Failed -> TaskEvaluatorResult.FAILURE
            is TaskEvaluationResult.Success -> TaskEvaluatorResult.SUCCESS
            is TaskEvaluationResult.Retry -> TaskEvaluatorResult.RETRY
        }
    }

    companion object {
        private const val TAG = "TaskEvaluator"
    }
}

