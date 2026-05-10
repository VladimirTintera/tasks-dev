package eu.tintera.tasks.core

import eu.tintera.tasks.*
import eu.tintera.tasks.core.data.ExecutableTask
import eu.tintera.tasks.core.data.TaskEvaluatorRepository
import eu.tintera.tasks.core.data.TaskProcessResult
import eu.tintera.tasks.core.migrations.TaskMigrator
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

@Suppress("UNCHECKED_CAST")
class TaskEvaluatorImpl(
    private val registryResolver: RegistryResolver,
    private val taskMigrator: TaskMigrator,
    private val taskScopeFactory: TaskScopeFactory,
    private val applicationScope: ApplicationScope,
    private val dispatchers: AppDispatchers,
    private val tagMapper: TagMapper,
    private val repository: TaskEvaluatorRepository,
    private val taskResultProcessor: TaskResultProcessor
) : TaskEvaluator {

    override suspend fun handle(
        id: Uuid,
        onForegroundInfo: suspend (ForegroundInfo) -> Boolean
    ): TaskEvaluatorResult {

        val task = repository.executableTask(id) ?: return TaskEvaluatorResult.FAILURE

        val registration = registryResolver.resolve<Any, Any, Any>(
            identifier = task.identifier
        ) ?: return TaskEvaluatorResult.FAILURE

        val migrationResult = taskMigrator.migrate(
            data = task,
            registration = registration
        )?.also {
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
            id = id,
            task = task,
            result = TaskResult.Failure,
            registration = registration,
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
                    finishedAt = parentEntity.finishedAt
                )
            }
        }

        val tags = tagMapper.parse(tags = task.tags)

        val result = taskScopeFactory.createForTask(
            taskId = id,
            data = typedInput,
            retryCount = task.runAttemptCount - 1,
            parentData = parents,
            onForegroundInfoProvided = onForegroundInfo,
            progressSerializer = registration.progressSerializer,
            scope = applicationScope + dispatchers.default,
            tags = tags.toSet(),
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
                e.printStackTrace()
                EventBus.send(TAG, "Task execution failed with error '${e.message}'")
                TaskResult.Failure
            }
        }

        return handleResult(
            id = id,
            task = task,
            result = result,
            registration = registration,
        )
    }

    private suspend fun handleResult(
        id: Uuid,
        task: ExecutableTask,
        result: TaskResult<Any>,
        registration: TaskRegistration<Any, Any, Any>
    ): TaskEvaluatorResult {

        withContext(NonCancellable) {
            taskResultProcessor.handleResult(
                result = TaskProcessResult(
                    id = id,
                    result = result,
                    repeatInterval = task.repeatInterval,
                    backoffCriteria = task.backoffCriteria,
                    retryCount = task.runAttemptCount - 1
                ), registration
            )
        }

        return when (result) {
            is TaskResult.Failure -> TaskEvaluatorResult.FAILURE
            is TaskResult.Success -> TaskEvaluatorResult.SUCCESS
            is TaskResult.Retry -> TaskEvaluatorResult.RETRY
        }
    }

    companion object {
        private const val TAG = "TaskEvaluator"
    }
}

