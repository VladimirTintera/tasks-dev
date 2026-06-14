package eu.tintera.background.tasks.core

import eu.tintera.background.tasks.BackoffCriteria
import eu.tintera.background.tasks.State
import kotlin.time.Duration
import kotlin.time.Instant
import kotlin.uuid.Uuid

data class ProcessableTask(
    val id: Uuid,
    val state: State,
    val initialDelay: Duration,
    val runAttemptCount: Int,
    val networkRequired: Boolean,
    val requiresDeviceIdle: Boolean,
    val repeatInterval: Duration?,
    val backoffCriteria: BackoffCriteria?,
    val processTime: Instant?
)