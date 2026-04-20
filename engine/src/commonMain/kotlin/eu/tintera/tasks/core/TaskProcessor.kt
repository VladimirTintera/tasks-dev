package eu.tintera.tasks.core

import eu.tintera.guard.ExecutionContextProvider
import eu.tintera.guard.invoke
import eu.tintera.tasks.EventBus
import eu.tintera.tasks.State
import eu.tintera.tasks.TaskResult
import eu.tintera.tasks.core.ExecutionResult.Canceled
import eu.tintera.tasks.core.ExecutionResult.Finished
import eu.tintera.tasks.core.data.Repository
import eu.tintera.tasks.core.data.Task
import eu.tintera.tasks.core.preconditions.PreconditionLostException
import eu.tintera.tasks.core.preconditions.TaskPreconditionController
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlin.coroutines.cancellation.CancellationException
import kotlin.uuid.Uuid

internal interface TaskProcessor {
    suspend fun run(task: Task)
}

internal class TaskProcessorImpl(
    private val repository: Repository,
    private val taskEvaluator: TaskEvaluator,
    private val executionContextProvider: ExecutionContextProvider,
    config: TaskProcessorConfig = TaskProcessorConfig(),
    private val preconditionController: TaskPreconditionController,
    private val taskResultProcessor: TaskResultProcessor
) : TaskProcessor {

    private val concurrencySemaphore = Semaphore(config.maxConcurrentTasks)

    override suspend fun run(task: Task) = coroutineScope {

        EventBus.send(TAG, "running task ${task.id}")

        val actualTask = MutableStateFlow<Task?>(task)

        val workflowJob = launch {

            if (waitForPreconditions(task)) concurrencySemaphore.withPermit {
                executeTask(actualTask.value ?: return@launch)
            }
        }

        val observeJob = launch {
            repository.task(task.id).onEach { actualTask.update { it } }
                .first { t ->
                    t == null || t.state.terminal()
                }
            workflowJob.cancelAndJoin()
        }

        workflowJob.join()
        observeJob.cancel()
    }

    private suspend fun waitForPreconditions(
        task: Task
    ): Boolean = when (preconditionController.waitForAll(task)) {
        TaskPreconditionController.WaitResult.SUCCESS -> {
            updateState(id = task.id, State.Enqueued, resetProcessTime = true)
            true
        }

        TaskPreconditionController.WaitResult.FAILED -> withContext(NonCancellable) {
            taskResultProcessor.handleResult(task, Finished(TaskResult.failure()))
            false
        }

        TaskPreconditionController.WaitResult.CANCELED -> withContext(NonCancellable) {
            taskResultProcessor.handleResult(task, Canceled)
            false
        }
    }

    private suspend fun executeTask(
        task: Task
    ) = executionContextProvider {

        updateState(id = task.id, state = State.Running, resetProcessTime = true)

        val taskResult = try {
            coroutineScope {

                val capabilityWatcher = launch {
                    val failedPreconditions = preconditionController.waitForUnmet(task)
                    this@coroutineScope.cancel(PreconditionLostException(failedPreconditions))
                }

                val result = taskEvaluator.handle(
                    task = task,
                    onForegroundInfo = { true }
                )

                capabilityWatcher.cancel()
                Finished(result)
            }
        } catch (e: PreconditionLostException) {
            EventBus.send(
                TAG,
                "Task interrupted '${task.identifier}' due to lost capability (${e.failedPreconditions}). Enqueueing back."
            )
            ExecutionResult.Yielded
        } catch (e: CancellationException) {
            // when canceled, do nothing. Invalid Running states are handled by sweep mechanism
            // Let it crash
            throw e
        } catch (e: Throwable) {
            EventBus.send(TAG, "Task failed '${task.identifier}'")
            Finished(TaskResult.failure())
        }

        withContext(NonCancellable) {
            EventBus.send(TAG, "Task finished '${task.identifier}', result = $taskResult")
            taskResultProcessor.handleResult(task, taskResult)
        }
    }

    private suspend fun updateState(
        id: Uuid,
        state: State,
        resetProcessTime: Boolean
    ) = repository.updateState(
        id = id,
        state = state,
        allowedSourceStates = state.allowedSourceStatesForChangeTo().toSet(),
        resetProcessTime = resetProcessTime
    )

    companion object {
        private const val TAG = "TaskProcessor"
    }
}