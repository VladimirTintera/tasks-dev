package eu.tintera.tasks.core

import eu.tintera.tasks.EventBus
import eu.tintera.tasks.ForegroundInfo
import eu.tintera.tasks.ParentData
import eu.tintera.tasks.TaskResult
import eu.tintera.tasks.core.data.Repository
import eu.tintera.tasks.core.data.Task
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.plus
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Instant

interface TaskEvaluator {
    suspend fun handle(
        task: Task,
        onForegroundInfo: suspend (ForegroundInfo) -> Boolean
    ): TaskResult<ByteArray>
}

@Suppress("UNCHECKED_CAST")
internal class TaskEvaluatorImpl(
    private val taskRegistry: TaskRegistry,
    private val repository: Repository,
    private val taskMigrator: TaskMigrator,
    private val taskScopeFactory: TaskScopeFactory,
    private val applicationScope: ApplicationScope,
    private val dispatchers: AppDispatchers
) : TaskEvaluator {

    override suspend fun handle(
        task: Task,
        onForegroundInfo: suspend (ForegroundInfo) -> Boolean
    ): TaskResult<ByteArray> {

        val registration = taskRegistry.resolve<Any, Any, Any>(task.identifier) ?: return TaskResult.failure()

        val typedInput = taskMigrator.migrate(
            task = task,
            registration = registration
        ) ?: return TaskResult.failure()

        val parents = repository.parentsFor(task.id).first().mapNotNull { parentEntity ->
            taskRegistry.resolve<Any, Any, Any>(parentEntity.identifier)?.let { parentRegistration ->
                ParentData(
                    id = parentEntity.id.toString(),
                    identifier = parentEntity.identifier,
                    data = parentEntity.outputData?.let {
                        parentRegistration.outputSerializer.decodeFromBytes(it)
                    },
                    finishedAt = task.finishedAt ?: Instant.DISTANT_PAST
                )
            }
        }

        val scope = taskScopeFactory.createForTask(
            taskId = task.id,
            data = typedInput,
            retryCount = task.runAttemptCount - 1,
            parentData = parents,
            onForegroundInfoProvided = onForegroundInfo,
            progressSerializer = registration.progressSerializer,
            scope = applicationScope + dispatchers.default
        )

        val typedResult = try {
            val handler = registration.factory()
            with(handler) {
                scope.run()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
            EventBus.send(TAG, "Task execution failed with error '${e.message}'")
            return TaskResult.Failure
        }



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

