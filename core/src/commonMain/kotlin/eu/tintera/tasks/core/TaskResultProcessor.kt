package eu.tintera.tasks.core

import eu.tintera.tasks.BackoffCriteria
import eu.tintera.tasks.State
import eu.tintera.tasks.core.data.TaskProcessResult
import kotlin.time.Clock

interface TaskResultProcessor {
    suspend fun handleResult(task: TaskProcessResult)
}

class TaskResultProcessorImpl(
    private val repository: TaskResultProcessorRepository,
    private val taskLifecycleObserver: CompositeTaskLifecycleObserver
) : TaskResultProcessor {

    override suspend fun handleResult(task: TaskProcessResult) {
        val now = Clock.System.now()
        when (val result = task.executionResult) {
            is ExecutionResult.EvaluatorResult -> {
                when (result.evaluatorResult) {
                    TaskEvaluatorResult.Failure -> {
                        val duration = task.repeatInterval
                        if (duration != null) repository.scheduleNextFromBeginning(
                            id = task.id,
                            state = State.Enqueued,
                            processTime = now + duration
                        )
                        else repository.failTask(
                            id = task.id,
                            state = State.Failed,
                            finishedAt = now
                        )
                    }

                    is TaskEvaluatorResult.Success -> {
                        val duration = task.repeatInterval

                        if (duration != null) repository.scheduleNextFromBeginning(
                            id = task.id,
                            state = State.Enqueued,
                            processTime = now + duration
                        )
                        else repository.successTask(
                            id = task.id,
                            state = State.Succeeded,
                            finishedAt = now,
                            outputData = result.evaluatorResult.bytes
                        )
                    }

                    TaskEvaluatorResult.Retry -> {
                        val backoff = (task.backoffCriteria ?: BackoffCriteria.DEFAULT).calculate(task.retryCount)
                        repository.scheduleNext(
                            id = task.id,
                            state = State.Enqueued,
                            processTime = now + backoff
                        )
                    }
                }
                taskLifecycleObserver.onCompleted(task.id, result.evaluatorResult.toTaskResult())
            }

            ExecutionResult.Yielded -> {
                repository.scheduleNext(
                    id = task.id,
                    state = State.Enqueued,
                    processTime = now
                )
                taskLifecycleObserver.onCanceled(task.id, "Precondition lost")
            }

            ExecutionResult.Canceled -> {
                repository.failTask(
                    id = task.id,
                    state = State.Cancelled,
                    finishedAt = now
                )
                taskLifecycleObserver.onCanceled(task.id, "Task canceled")
            }
        }
    }
}