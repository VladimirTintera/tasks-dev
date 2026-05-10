package eu.tintera.tasks.core.data

import eu.tintera.tasks.BackoffCriteria
import eu.tintera.tasks.TaskRegistration
import kotlin.time.Duration
import kotlin.uuid.Uuid

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