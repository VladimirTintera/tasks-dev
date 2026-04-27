package eu.tintera.tasks.core

import eu.tintera.guard.ExecutionContextProvider
import eu.tintera.guard.invoke
import eu.tintera.tasks.EventBus
import eu.tintera.tasks.core.constraints.ConstraintController
import eu.tintera.tasks.core.constraints.ConstraintLostException
import eu.tintera.tasks.core.data.ExecutableTask
import eu.tintera.tasks.core.data.TaskProcessResult
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
    private val taskEvaluator: TaskEvaluator,
    private val executionContextProvider: ExecutionContextProvider,
    config: TaskProcessorConfig = TaskProcessorConfig(),
    private val preconditionController: ConstraintController,
    private val taskResultProcessor: TaskResultProcessor,
    private val taskLifecycleObserver: CompositeTaskLifecycleObserver,
    private val repository: TaskProcessorRepository
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
    ): Boolean = preconditionController.waitForAll(task).also {
        if (it) {
            repository.updateEnqueuedState(
                id = id,
                allowedSourceStates = runningStates
            )
            taskLifecycleObserver.onPreconditionsSucceeded(id)
        } else {
            taskLifecycleObserver.onPreconditionsFailed(id)
        }
    }

    private suspend fun executeTask(
        id: Uuid,
        task: StateFlow<ProcessableTask?>,
        executableTask: ExecutableTask
    ) = executionContextProvider {

        taskLifecycleObserver.onStarted(id)

        val retryCount = executableTask.runAttemptCount

        repository.updateRunningState(
            id = id,
            runAttemptCount = retryCount + 1,
            allowedSourceStates = runningStates
        )

        val evaluatorResult: ExecutionResult = try {
            coroutineScope {

                val capabilityWatcher = launch {
                    preconditionController.waitForUnmet(task)
                    this@coroutineScope.cancel(ConstraintLostException())
                }

                val result = taskEvaluator.handle(
                    id = id,
                    task = executableTask,
                    onForegroundInfo = { true }
                )

                capabilityWatcher.cancel()
                ExecutionResult.EvaluatorResult(result)
            }
        } catch (e: ConstraintLostException) {
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

    companion object {
        private const val TAG = "TaskProcessor"
    }
}