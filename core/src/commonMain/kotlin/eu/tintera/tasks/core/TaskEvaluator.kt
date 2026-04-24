package eu.tintera.tasks.core

import eu.tintera.tasks.*
import eu.tintera.tasks.core.data.ExecutableTask
import eu.tintera.tasks.core.data.Repository
import eu.tintera.tasks.core.migrations.TaskMigrator
import kotlinx.coroutines.plus
import kotlin.coroutines.cancellation.CancellationException
import kotlin.uuid.Uuid

sealed interface TaskEvaluatorResult<out Output> {
    class Success<Output>(val outputData: Output, val bytes: ByteArray) : TaskEvaluatorResult<Output>
    data object Failure : TaskEvaluatorResult<Nothing>
    data object Retry : TaskEvaluatorResult<Nothing>

    fun toTaskResult(): TaskResult<Output> = when (this) {
        is Success -> TaskResult.success(outputData)
        Failure -> TaskResult.failure()
        Retry -> TaskResult.retry()
    }
}


interface TaskEvaluator {
    suspend fun handle(
        id: Uuid,
        task: ExecutableTask,
        onForegroundInfo: suspend (ForegroundInfo) -> Boolean
    ): TaskEvaluatorResult<Any>
}

@Suppress("UNCHECKED_CAST")
class TaskEvaluatorImpl(
    private val taskRegistry: RegistryResolver,
    private val repository: Repository,
    private val taskMigrator: TaskMigrator,
    private val taskScopeFactory: TaskScopeFactory,
    private val applicationScope: ApplicationScope,
    private val dispatchers: AppDispatchers
) : TaskEvaluator {

    override suspend fun handle(
        id: Uuid,
        task: ExecutableTask,
        onForegroundInfo: suspend (ForegroundInfo) -> Boolean
    ): TaskEvaluatorResult<Any> {

        val registration = taskRegistry.resolve<Any, Any, Any>(task.identifier) ?: return TaskEvaluatorResult.Failure

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
        } ?: return TaskResult.Failure.toResult(registration)

        val parents = repository.parentsDataFor(id).mapNotNull { parentEntity ->
            taskRegistry.resolve<Any, Any, Any>(parentEntity.identifier)?.let { parentRegistration ->
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

        return taskScopeFactory.createForTask(
            taskId = id,
            data = typedInput,
            retryCount = task.runAttemptCount - 1,
            parentData = parents,
            onForegroundInfoProvided = onForegroundInfo,
            progressSerializer = registration.progressSerializer,
            scope = applicationScope + dispatchers.default,
            tags = task.tags
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
            }.toResult(registration)
        }
    }

    private fun TaskResult<Any>.toResult(
        registration: TaskRegistration<Any, Any, Any>
    ) = when (this) {
        TaskResult.Failure -> TaskEvaluatorResult.Failure
        TaskResult.Retry -> TaskEvaluatorResult.Retry
        is TaskResult.Success -> TaskEvaluatorResult.Success(
            outputData = outputData,
            bytes = registration.outputSerializer.encodeToBytes(outputData)
        )
    }


    companion object {
        private const val TAG = "TaskEvaluator"
    }
}

