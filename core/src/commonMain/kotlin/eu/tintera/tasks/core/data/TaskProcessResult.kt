package eu.tintera.tasks.core.data

import eu.tintera.tasks.BackoffCriteria
import eu.tintera.tasks.core.ExecutionResult
import kotlin.time.Duration
import kotlin.uuid.Uuid

data class TaskProcessResult(
    val id: Uuid,
    val executionResult: ExecutionResult,
    val repeatInterval: Duration?,
    val backoffCriteria: BackoffCriteria?,
    val retryCount: Int
)