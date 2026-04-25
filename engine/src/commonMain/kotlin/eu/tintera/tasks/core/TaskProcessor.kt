package eu.tintera.tasks.core

import eu.tintera.guard.ExecutionContextProvider
import eu.tintera.guard.invoke
import eu.tintera.tasks.EventBus
import eu.tintera.tasks.State
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
    private val taskResultProcessor: TaskResultProcessor,
    private val taskLifecycleObserver: CompositeTaskLifecycleObserver
) : TaskProcessor {

    private val concurrencySemaphore = Semaphore(config.maxConcurrentTasks)

    private fun ProcessableTask?.isDisrupted() = this == null || state.terminal()

    override suspend fun run(id: Uuid) = coroutineScope {

        EventBus.send(TAG, "running task $id")

        val taskFlowScope = CoroutineScope(coroutineContext + Job(coroutineContext.job))

        try {

            val task = repository.processableTask(id).stateIn(taskFlowScope)

            if (task.value.isDisrupted()) return@coroutineScope

            val workflowJob = launch {

                taskLifecycleObserver.onWaitingForPreconditions(id)

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

                EventBus.send(TAG, "Task finished '$id'")
            }

            val observeJob = launch {
                task.first { it.isDisrupted() }
                workflowJob.cancelAndJoin()
            }

            workflowJob.join()
            observeJob.cancel()
        } finally {
            taskFlowScope.cancel()
        }
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
                taskLifecycleObserver.onPreconditionsSucceeded(id)
                true
            }

            TaskPreconditionController.WaitResult.FAILED -> withContext(NonCancellable) {
                taskLifecycleObserver.onPreconditionsFailed(id)
                task.value?.also {
                    taskResultProcessor.handleResult(
                        it.toTaskProcessResult(
                            ExecutionResult.EvaluatorResult(
                                TaskEvaluatorResult.Failure
                            )
                        )
                    )
                }
                false
            }

            TaskPreconditionController.WaitResult.CANCELED -> withContext(NonCancellable) {
                taskLifecycleObserver.onCanceled(id)
                task.value?.also {
                    taskResultProcessor.handleResult(it.toTaskProcessResult(ExecutionResult.Canceled))
                }
                false
            }
        }
    }

    private fun ProcessableTask.toTaskProcessResult(
        result: ExecutionResult
    ) = TaskProcessResult(
        id = id,
        executionResult = result,
        repeatInterval = repeatInterval,
        backoffCriteria = backoffCriteria,
        retryCount = runAttemptCount
    )

    private suspend fun executeTask(
        id: Uuid,
        task: StateFlow<ProcessableTask?>,
        executableTask: ExecutableTask
    ) = executionContextProvider {

        taskLifecycleObserver.onStarted(id)

        val retryCount = executableTask.runAttemptCount

        updateState(
            id = id,
            state = State.Running,
            resetProcessTime = true,
            runAttemptCount = retryCount + 1
        )

        val evaluatorResult: ExecutionResult = try {
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
                ExecutionResult.EvaluatorResult(result)
            }
        } catch (e: PreconditionLostException) {
            EventBus.send(
                TAG,
                "Task interrupted '${executableTask.identifier}' due to lost capability. Enqueueing back."
            )
            taskLifecycleObserver.onCanceled(id, "Precondition lost")
            ExecutionResult.Yielded
        } catch (e: CancellationException) {
            // when canceled, do nothing. Invalid Running states are handled by sweep mechanism
            // Let it crash
            taskLifecycleObserver.onCanceled(id, "Context cancelled")
            throw e
        } catch (e: Throwable) {
            EventBus.send(TAG, "Task failed '${executableTask.identifier}'")
            ExecutionResult.EvaluatorResult(TaskEvaluatorResult.Failure)
        }

        task.value?.let {
            withContext(NonCancellable) {
                EventBus.send(TAG, "Task finished '${executableTask.identifier}', result = $evaluatorResult")
                taskResultProcessor.handleResult(
                    TaskProcessResult(
                        id = id,
                        executionResult = evaluatorResult,
                        repeatInterval = it.repeatInterval,
                        backoffCriteria = it.backoffCriteria,
                        retryCount = retryCount
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