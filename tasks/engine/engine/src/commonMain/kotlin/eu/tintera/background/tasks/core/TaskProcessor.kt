package eu.tintera.background.tasks.core

import eu.tintera.background.guard.ExecutionContextProvider
import eu.tintera.background.guard.invoke
import eu.tintera.background.tasks.EventBus
import eu.tintera.background.tasks.core.constraints.ConstraintController
import eu.tintera.background.tasks.core.constraints.ConstraintLostException
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlin.uuid.Uuid

internal interface TaskProcessor {
    suspend fun run(id: Uuid)
}

internal class TaskProcessorImpl(
    private val taskEvaluator: TaskEvaluator,
    private val executionContextProvider: ExecutionContextProvider,
    config: TaskProcessorConfig = TaskProcessorConfig(),
    private val preconditionController: ConstraintController,
    private val taskLifecycleObserver: CompositeTaskLifecycleObserver,
    private val repository: TaskProcessorRepository,
    private val clock: Clock,
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
                        task = task
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
        task: StateFlow<ProcessableTask?>
    ) = executionContextProvider {

        taskLifecycleObserver.onStarted(id)

        repository.run(
            id = id,
            allowedSourceStates = runningStates
        )

        try {
            coroutineScope {

                val capabilityWatcher = launch {
                    preconditionController.waitForUnmet(task)
                    this@coroutineScope.cancel(ConstraintLostException())
                }

                val result = taskEvaluator.handle(
                    id = id,
                    onForegroundInfo = { true }
                )

                capabilityWatcher.cancel()
                result
            }
        } catch (_: ConstraintLostException) {
            taskLifecycleObserver.onCanceled(id, "Precondition lost")
            repository.enqueue(
                id = id,
                processTime = clock.now(),
                allowedSourceStates = runningStates
            )
            null
        } catch (e: CancellationException) {
            // when canceled, do nothing. Invalid Running states are handled by sweep mechanism
            // Let it crash
            taskLifecycleObserver.onCanceled(id, "Context cancelled")
            throw e
        } catch (_: Throwable) {
            EventBus.send(TAG, "Task failed '${id}'")
            repository.fail(id = id)
        }
    }

    companion object {
        private const val TAG = "TaskProcessor"
    }
}