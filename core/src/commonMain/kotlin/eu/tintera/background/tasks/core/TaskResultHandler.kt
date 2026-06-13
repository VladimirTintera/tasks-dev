package eu.tintera.background.tasks.core

import eu.tintera.background.tasks.BackoffCriteria
import eu.tintera.background.tasks.State
import eu.tintera.background.tasks.TaskRegistration
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.uuid.Uuid

interface TaskResultHandler {
    suspend fun handleResult(result: TaskEvaluationResult)
}

sealed interface TaskEvaluationResult {
    val id: Uuid

    data class Failed(
        override val id: Uuid,
        val repeatInterval: Duration?,
    ) : TaskEvaluationResult

    data class Success(
        override val id: Uuid,
        val registration: TaskRegistration<Any, Any, Any>,
        val repeatInterval: Duration?,
        val outputData: Any,
    ) : TaskEvaluationResult

    data class Retry(
        override val id: Uuid,
        val backoffCriteria: BackoffCriteria?,
        val retryCount: Int
    ) : TaskEvaluationResult
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
                val backoff = (result.backoffCriteria ?: defaultBackoffCriteria).calculate(result.retryCount).also {
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
