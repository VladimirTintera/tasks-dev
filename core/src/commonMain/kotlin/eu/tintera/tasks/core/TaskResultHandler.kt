package eu.tintera.tasks.core

import eu.tintera.tasks.BackoffCriteria
import eu.tintera.tasks.State
import eu.tintera.tasks.core.data.TaskEvaluationResult
import kotlin.time.Clock

interface TaskResultHandler {
    suspend fun handleResult(result: TaskEvaluationResult)
}

internal class TaskResultHandlerImpl(
    private val repository: TaskResultProcessorRepository,
) : TaskResultHandler {

    override suspend fun handleResult(
        result: TaskEvaluationResult,
    ) {
        val now = Clock.System.now()

        when (result) {
            is TaskEvaluationResult.Failed -> {
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

            is TaskEvaluationResult.Retry -> {
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

            is TaskEvaluationResult.Success -> {
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
                    outputData = result.registration.outputSerializer.encodeToBytes(result.outputData),
                    allowedSourceStates = runningStates
                )
            }
        }
    }
}
