package eu.tintera.tasks.core.data

import eu.tintera.tasks.BackoffCriteria
import eu.tintera.tasks.State
import kotlin.time.Duration

data class ProcessableTask(
    val state: State,
    val initialDelay: Duration,
    val runAttemptCount: Int,
    val networkRequired: Boolean,
    val requiresDeviceIdle: Boolean,
    val repeatInterval: Duration?,
    val backoffCriteria: BackoffCriteria?
)