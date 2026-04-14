package eu.tintera.tasks.core.data

import eu.tintera.tasks.BackoffCriteria
import eu.tintera.tasks.Data
import eu.tintera.tasks.State
import eu.tintera.tasks.core.DEFAULT
import kotlin.time.Duration
import kotlin.time.Instant
import kotlin.uuid.Uuid

data class Task(
    val id: Uuid,
    val identifier: String,
    val uniqueName: String,
    val runAttemptCount: Int,
    val initialDelay: Duration,
    val processTime: Instant?,
    val state: State,
    val inputData: Data,
    val outputData: Data,
    val networkRequired: Boolean,
    val createdAt: Instant,
    val finishedAt: Instant?,
    val repeatInterval: Duration?,
    val backoffCriteria: BackoffCriteria?,
    val progressData: Data?,
    val retentionDelay: Duration,
    val requiresDeviceIdle: Boolean
)

internal val Task.backoffCriteriaOrDefault: BackoffCriteria get() = backoffCriteria ?: BackoffCriteria.DEFAULT