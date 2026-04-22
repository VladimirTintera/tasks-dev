package eu.tintera.tasks.core

import eu.tintera.tasks.BackoffCriteria
import eu.tintera.tasks.State
import eu.tintera.tasks.core.data.Repository
import eu.tintera.tasks.core.data.TaskProcessResult
import kotlin.time.Clock

interface TaskResultProcessor {
    suspend fun handleResult(task: TaskProcessResult)
}

class TaskResultProcessorImpl(
    private val repository: Repository,
    private val taskLifecycleObserver: CompositeTaskLifecycleObserver
) : TaskResultProcessor {

    override suspend fun handleResult(task: TaskProcessResult) {
        val now = Clock.System.now()
        when (val result = task.executionResult) {
            is ExecutionResult.EvaluatorResult -> {
                when (result.evaluatorResult) {
                    TaskEvaluatorResult.Failure -> {
                        val duration = task.repeatInterval
                        if (duration != null) repository.updateNextRun(
                            id = task.id,
                            state = State.Enqueued,
                            processTime = now + duration,
                            progressData = null,
                            runAttemptCount = 0
                        )
                        else repository.updateTerminatingState(
                            id = task.id,
                            state = State.Failed,
                            finishedAt = now,
                            outputData = null
                        )
                    }

                    is TaskEvaluatorResult.Success -> {
                        val duration = task.repeatInterval

                        if (duration != null) repository.updateNextRun(
                            id = task.id,
                            state = State.Enqueued,
                            processTime = now + duration,
                            progressData = null,
                            runAttemptCount = 0
                        )
                        else repository.updateTerminatingState(
                            id = task.id,
                            state = State.Succeeded,
                            finishedAt = now,
                            outputData = result.evaluatorResult.bytes
                        )
                    }

                    TaskEvaluatorResult.Retry -> {
                        val backoff = (task.backoffCriteria ?: BackoffCriteria.DEFAULT).calculate(task.retryCount)
                        repository.updateNextRun(
                            id = task.id,
                            state = State.Enqueued,
                            processTime = now + backoff,
                            progressData = null,
                            runAttemptCount = null
                        )
                    }
                }
                taskLifecycleObserver.onCompleted(task.id, result.evaluatorResult.toTaskResult())
            }

            ExecutionResult.Yielded -> {
                repository.updateNextRun(
                    id = task.id,
                    state = State.Enqueued,
                    processTime = now,
                    progressData = null,
                    runAttemptCount = null
                )
                taskLifecycleObserver.onCanceled(task.id, "Precondition lost")
            }

            ExecutionResult.Canceled -> {
                repository.updateTerminatingState(
                    id = task.id,
                    state = State.Cancelled,
                    finishedAt = now,
                    outputData = null,
                )
                taskLifecycleObserver.onCanceled(task.id, "Task canceled")
            }
        }
    }
}