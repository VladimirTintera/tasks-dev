package eu.tintera.tasks

import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

data class TaskRequest<I>(
    val identifier: String,
    val initialDelay: Duration = Duration.ZERO,
    val data: I,
    val constraints: Constraints = Constraints.EMPTY,
    val tags: Set<String> = emptySet(),
    val backoffCriteria: BackoffCriteria? = null,
    val keepResultsForAtLeast: Duration = 24.hours
)

inline fun <reified T : TaskHandler<I, *, *>, reified I> taskRequest(
    data: I,
    identifier: String = T::class.fullName,
    initialDelay: Duration = Duration.ZERO,
    constraints: Constraints = Constraints.EMPTY,
    tags: Set<String> = emptySet(),
    backoffCriteria: BackoffCriteria? = null,
    keepResultsForAtLeast: Duration = 24.hours
): TaskRequest<I> = TaskRequest(
    identifier = identifier,
    initialDelay = initialDelay,
    data = data,
    constraints = constraints,
    tags = tags,
    backoffCriteria = backoffCriteria,
    keepResultsForAtLeast = keepResultsForAtLeast
)

inline fun <reified T : TaskHandler<Data, *, *>> taskRequest(
    identifier: String = T::class.fullName,
    data: Data = Data.EMPTY,
    initialDelay: Duration = Duration.ZERO,
    constraints: Constraints = Constraints.EMPTY,
    tags: Set<String> = emptySet(),
    backoffCriteria: BackoffCriteria? = null,
    keepResultsForAtLeast: Duration = 24.hours
): TaskRequest<Data> = TaskRequest(
    identifier = identifier,
    initialDelay = initialDelay,
    data = data,
    constraints = constraints,
    tags = tags,
    backoffCriteria = backoffCriteria,
    keepResultsForAtLeast = keepResultsForAtLeast
)