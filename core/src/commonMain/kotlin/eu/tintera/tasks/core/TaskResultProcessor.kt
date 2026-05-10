package eu.tintera.tasks.core

import eu.tintera.tasks.BackoffCriteria
import eu.tintera.tasks.State
import eu.tintera.tasks.TaskRegistration
import eu.tintera.tasks.TaskResult
import eu.tintera.tasks.core.data.TaskProcessResult
import kotlin.time.Clock

interface TaskResultProcessor {
    suspend fun handleResult(result: TaskProcessResult, registration: TaskRegistration<Any, Any, Any>)
}

class TaskResultProcessorImpl(
    private val repository: TaskResultProcessorRepository,
) : TaskResultProcessor {

    override suspend fun handleResult(
        result: TaskProcessResult,
        registration: TaskRegistration<Any, Any, Any>
    ) {
        val now = Clock.System.now()
        when (val taskResult = result.result) {

            TaskResult.Failure -> {
                val duration = result.repeatInterval
                if (duration != null) repository.scheduleNextFromBeginning(
                    id = result.id,
                    state = State.Enqueued,
                    processTime = now + duration,
                    allowedSourceStates = runningStates
                )
                else repository.failTask(
                    id = result.id,
                    state = State.Failed,
                    finishedAt = now,
                    allowedSourceStates = runningStates
                )
            }

            is TaskResult.Success -> {
                val duration = result.repeatInterval

                if (duration != null) repository.scheduleNextFromBeginning(
                    id = result.id,
                    state = State.Enqueued,
                    processTime = now + duration,
                    allowedSourceStates = runningStates
                )
                else repository.successTask(
                    id = result.id,
                    state = State.Succeeded,
                    finishedAt = now,
                    outputData = registration.outputSerializer.encodeToBytes(taskResult.outputData),
                    allowedSourceStates = runningStates
                )
            }

            TaskResult.Retry -> {
                val backoff = (result.backoffCriteria ?: BackoffCriteria.DEFAULT).calculate(result.retryCount).also {
                    println("Backoff delay = $it")
                }
                repository.scheduleNext(
                    id = result.id,
                    state = State.Enqueued,
                    processTime = now + backoff,
                    allowedSourceStates = runningStates
                )
            }
        }
    }
}
