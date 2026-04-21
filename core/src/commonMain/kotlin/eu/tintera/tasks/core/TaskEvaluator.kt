package eu.tintera.tasks.core

import eu.tintera.tasks.EventBus
import eu.tintera.tasks.ForegroundInfo
import eu.tintera.tasks.ParentData
import eu.tintera.tasks.TaskResult
import eu.tintera.tasks.core.data.ExecutableTask
import eu.tintera.tasks.core.data.Repository
import eu.tintera.tasks.core.migrations.TaskMigrator
import kotlinx.coroutines.plus
import kotlin.coroutines.cancellation.CancellationException
import kotlin.uuid.Uuid

interface TaskEvaluator {
    suspend fun handle(
        id: Uuid,
        task: ExecutableTask,
        onForegroundInfo: suspend (ForegroundInfo) -> Boolean
    ): TaskResult<ByteArray>
}

@Suppress("UNCHECKED_CAST")
class TaskEvaluatorImpl(
    private val taskRegistry: TaskRegistry,
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
    ): TaskResult<ByteArray> {

        val registration = taskRegistry.resolve<Any, Any, Any>(task.identifier) ?: return TaskResult.failure()

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
        } ?: return TaskResult.failure()

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

        val scope = taskScopeFactory.createForTask(
            taskId = id,
            data = typedInput,
            retryCount = task.runAttemptCount - 1,
            parentData = parents,
            onForegroundInfoProvided = onForegroundInfo,
            progressSerializer = registration.progressSerializer,
            scope = applicationScope + dispatchers.default
        )

        val typedResult = try {
            with(registration.factory()) {
                scope.run()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
            EventBus.send(TAG, "Task execution failed with error '${e.message}'")
            return TaskResult.Failure
        }

        scope.flushProgressAndClose()

        return when (typedResult) {
            is TaskResult.Success -> {
                val rawOutput = registration.outputSerializer.encodeToBytes(typedResult.outputData)
                TaskResult.Success(rawOutput)
            }

            TaskResult.Failure -> TaskResult.Failure
            TaskResult.Retry -> TaskResult.Retry
        }
    }


    companion object {
        private const val TAG = "TaskEvaluator"
    }
}

