package eu.tintera.tasks.core.data

import eu.tintera.tasks.BackoffCriteria
import eu.tintera.tasks.TaskResult
import eu.tintera.tasks.core.TaskEvaluatorResult
import kotlin.time.Duration
import kotlin.uuid.Uuid

data class TaskProcessResult(
    val id: Uuid,
    val result: TaskResult<Any>,
    val repeatInterval: Duration?,
    val backoffCriteria: BackoffCriteria?,
    val retryCount: Int
)