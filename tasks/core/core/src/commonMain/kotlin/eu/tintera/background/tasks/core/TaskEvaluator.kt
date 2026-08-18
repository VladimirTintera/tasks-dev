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

        // Failed, not Retry — deliberately. A handler the application stopped using (replaced by
        // another one, or dropped altogether) disappears from the registry, but tasks scheduled by
        // it earlier are still sitting in the queue. Those have to fail for good, otherwise they
        // would keep waking up forever.
        //
        // The race with application startup (the system runs a task before the consumer has built
        // its Koin) is handled by the registry's warmup window, which makes resolve wait. When even
        // that is not enough it is a matter of configuration:
        // TaskManagerConfiguration.registryWarmupTimeout.
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
                    "Either it was scheduled by a handler the application no longer registers, or the " +
                    "identifier in the registration does not match the one in TaskRequest. If the " +
                    "application starts slowly, consider raising " +
                    "TaskManagerConfiguration.registryWarmupTimeout."
            }
        }

        val migrationResult = runCatching {
            taskMigrator.migrate(data = task, registration = registration)
        }.getOrElse { e ->
            // Missing migration path, or a downgrade (the task was written by a newer version of
            // the application and is being read by an older one after a rollback). Letting this
            // escape would bring the worker down, so the task ends as Failed instead and the queue
            // keeps moving.
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

