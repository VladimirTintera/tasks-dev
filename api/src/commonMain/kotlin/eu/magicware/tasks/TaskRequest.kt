package eu.tintera.tasks

import kotlin.reflect.KClass
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

data class TaskRequest(
    val handler: KClass<out TaskHandler>,
    val initialDelay: Duration = Duration.Companion.ZERO,
    val data: Data = Data.EMPTY,
    val constraints: Constraints = Constraints.EMPTY,
    val tags: Set<String> = emptySet(),
    val backoffCriteria: BackoffCriteria? = null,
    val keepResultsForAtLeast: Duration = 24.hours
)