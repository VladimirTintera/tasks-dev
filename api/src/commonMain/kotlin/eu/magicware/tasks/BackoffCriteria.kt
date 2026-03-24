package eu.tintera.tasks

import kotlin.time.Duration

data class BackoffCriteria(
    val backoffPolicy: BackoffPolicy,
    val delay: Duration
) {
    companion object
}