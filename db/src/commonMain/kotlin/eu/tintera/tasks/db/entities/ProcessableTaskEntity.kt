package eu.tintera.tasks.db.entities

import eu.tintera.tasks.db.BackoffCriteria
import eu.tintera.tasks.db.State
import kotlin.time.Duration
import kotlin.time.Instant

data class ProcessableTaskEntity(
    val state: State,
    val initialDelay: Duration,
    val runAttemptCount: Int,
    val networkRequired: Boolean,
    val requiresDeviceIdle: Boolean,
    val repeatInterval: Duration?,
    val backoffCriteria: BackoffCriteria?,
    val processTime: Instant?
)