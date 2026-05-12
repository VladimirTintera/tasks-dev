package eu.tintera.tasks.db.entities

import eu.tintera.tasks.db.BackoffCriteriaDb
import eu.tintera.tasks.db.StateDb
import kotlin.time.Duration
import kotlin.time.Instant

data class ProcessableTaskEntity(
    val state: StateDb,
    val initialDelay: Duration,
    val runAttemptCount: Int,
    val networkRequired: Boolean,
    val requiresDeviceIdle: Boolean,
    val repeatInterval: Duration?,
    val backoffCriteria: BackoffCriteriaDb?,
    val processTime: Instant?
)