package eu.tintera.tasks.core

import eu.tintera.guard.ExecutionContextProvider
import eu.tintera.guard.invoke
import eu.tintera.tasks.EventBus
import eu.tintera.tasks.State
import eu.tintera.tasks.TaskResult
import eu.tintera.tasks.core.ExecutionResult.Canceled
import eu.tintera.tasks.core.ExecutionResult.Finished
import eu.tintera.tasks.core.data.ExecutableTask
import eu.tintera.tasks.core.data.ProcessableTask
import eu.tintera.tasks.core.data.Repository
import eu.tintera.tasks.core.data.TaskProcessResult
import eu.tintera.tasks.core.preconditions.PreconditionLostException
import eu.tintera.tasks.core.preconditions.TaskPreconditionController
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlin.coroutines.cancellation.CancellationException
import kotlin.uuid.Uuid

internal interface TaskProcessor {
    suspend fun run(id: Uuid)
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

    private fun ProcessableTask?.isDisrupted() = this == null || state.terminal()

    override suspend fun run(id: Uuid) = coroutineScope {

        EventBus.send(TAG, "running task $id")

        val task = repository.processableTask(id).stateIn(this)

        if (task.value.isDisrupted()) return@coroutineScope

        val workflowJob = launch {

            val preconditionsValid = waitForPreconditions(
                id = id,
                task = task
            )

            if (preconditionsValid) concurrencySemaphore.withPermit {
                executeTask(
                    id = id,
                    task = task,
                    executableTask = repository.executableTask(id) ?: return@launch
                )
            }
        }

        val observeJob = launch {
            task.first { it.isDisrupted() }
            workflowJob.cancelAndJoin()
        }

        workflowJob.join()
        observeJob.cancel()
    }

    private suspend fun waitForPreconditions(
        id: Uuid,
        task: StateFlow<ProcessableTask?>
    ): Boolean {
        return when (preconditionController.waitForAll(task)) {
            TaskPreconditionController.WaitResult.SUCCESS -> {
                updateState(
                    id = id,
                    state = State.Enqueued,
                    resetProcessTime = true,
                    runAttemptCount = null
                )
                true
            }

            TaskPreconditionController.WaitResult.FAILED -> withContext(NonCancellable) {
                task.value?.let {
                    taskResultProcessor.handleResult(
                        TaskProcessResult(
                            id = id,
                            executionResult = Finished(TaskResult.failure()),
                            repeatInterval = it.repeatInterval,
                            backoffCriteria = it.backoffCriteria,
                            retryCount = it.runAttemptCount
                        )
                    )
                }
                false
            }

            TaskPreconditionController.WaitResult.CANCELED -> withContext(NonCancellable) {
                task.value?.let {
                    taskResultProcessor.handleResult(
                        TaskProcessResult(
                            id = id,
                            executionResult = Canceled,
                            repeatInterval = it.repeatInterval,
                            backoffCriteria = it.backoffCriteria,
                            retryCount = it.runAttemptCount
                        )
                    )
                }
                false
            }
        }
    }

    private suspend fun executeTask(
        id: Uuid,
        task: StateFlow<ProcessableTask?>,
        executableTask: ExecutableTask
    ) = executionContextProvider {

        updateState(
            id = id,
            state = State.Running,
            resetProcessTime = true,
            runAttemptCount = executableTask.runAttemptCount + 1
        )

        val taskResult = try {
            coroutineScope {

                val capabilityWatcher = launch {
                    preconditionController.waitForUnmet(task)
                    this@coroutineScope.cancel(PreconditionLostException())
                }

                val result = taskEvaluator.handle(
                    id = id,
                    task = executableTask,
                    onForegroundInfo = { true }
                )

                capabilityWatcher.cancel()
                Finished(result)
            }
        } catch (e: PreconditionLostException) {
            EventBus.send(
                TAG,
                "Task interrupted '${executableTask.identifier}' due to lost capability. Enqueueing back."
            )
            ExecutionResult.Yielded
        } catch (e: CancellationException) {
            // when canceled, do nothing. Invalid Running states are handled by sweep mechanism
            // Let it crash
            throw e
        } catch (e: Throwable) {
            EventBus.send(TAG, "Task failed '${executableTask.identifier}'")
            Finished(TaskResult.failure())
        }

        task.value?.let {
            withContext(NonCancellable) {
                EventBus.send(TAG, "Task finished '${executableTask.identifier}', result = $taskResult")
                taskResultProcessor.handleResult(
                    TaskProcessResult(
                        id = id,
                        executionResult = taskResult,
                        repeatInterval = it.repeatInterval,
                        backoffCriteria = it.backoffCriteria,
                        retryCount = it.runAttemptCount
                    )
                )
            }
        }
    }

    private suspend fun updateState(
        id: Uuid,
        state: State,
        resetProcessTime: Boolean,
        runAttemptCount: Int?
    ) = repository.updateState(
        id = id,
        state = state,
        allowedSourceStates = state.allowedSourceStatesForChangeTo().toSet(),
        resetProcessTime = resetProcessTime,
        runAttemptCount = runAttemptCount
    )

    companion object {
        private const val TAG = "TaskProcessor"
    }
}