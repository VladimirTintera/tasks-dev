package eu.tintera.tasks.compat

import eu.tintera.tasks.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

inline fun <reified T : TaskHandler<Data, *, *>> taskRequest(
    identifier: String,
    data: Data = Data.EMPTY,
    initialDelay: Duration = Duration.ZERO,
    constraints: Constraints = Constraints.EMPTY,
    tags: Set<Tag> = emptySet(),
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