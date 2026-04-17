package eu.tintera.tasks.core

import eu.tintera.tasks.EventBus
import eu.tintera.tasks.ForegroundInfo
import eu.tintera.tasks.ParentData
import eu.tintera.tasks.TaskInfo
import eu.tintera.tasks.TaskResult
import eu.tintera.tasks.TaskScope
import eu.tintera.tasks.core.data.Repository
import eu.tintera.tasks.core.data.Task
import eu.tintera.tasks.core.data.toTaskInfo
import kotlinx.coroutines.flow.first
import kotlin.coroutines.cancellation.CancellationException
import kotlin.uuid.Uuid

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
    private val tasksMigrator: TasksMigrator
) : TaskEvaluator {

    override suspend fun handle(
        task: Task,
        onForegroundInfo: suspend (ForegroundInfo) -> Boolean
    ): TaskResult<ByteArray> {

        val registration = taskRegistry.resolve<Any?, Any?, Any?>(task.identifier) ?: return TaskResult.failure()

        val typedInput = tasksMigrator.migrate(
            task = task,
            registration = registration
        )

        val parents = repository.parentsFor(task.id).first().map { parentEntity ->

            val parentRegistration = taskRegistry.resolve<Any, Any?, Any?>(parentEntity.identifier)
                ?: error("Registration for parent '${parentEntity.identifier}' is missing!")

            ParentData(
                id = parentEntity.id.toString(),
                identifier = parentEntity.identifier,
                data = parentEntity.outputData?.let {
                    parentRegistration.outputSerializer.decodeFromBytes(it)
                }
            )
        }

        val scope = object : TaskScope<Any?, Any?> {
            override val taskId: Uuid = task.id
            override val data: Any? = typedInput
            override val retryCount: Int = task.runAttemptCount - 1
            override val parents: List<ParentData> = parents

            override suspend fun setForegroundInfo(foregroundInfo: ForegroundInfo): Boolean {
                return onForegroundInfo(foregroundInfo)
            }

            override suspend fun setProgress(data: Any?) {
                val rawProgress = data?.let {
                    registration.progressSerializer.encodeToBytes(it)
                }

                repository.updateProgressData(taskId, rawProgress)
            }
        }

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

