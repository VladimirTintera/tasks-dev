package eu.tintera.tasks.compat

import eu.tintera.tasks.BackoffCriteria
import eu.tintera.tasks.Constraints
import eu.tintera.tasks.TagsRequest
import eu.tintera.tasks.TaskHandler
import eu.tintera.tasks.TaskRequest
import eu.tintera.tasks.fullName
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

inline fun <reified T : TaskHandler<Data, *, *>> taskRequest(
    identifier: String = T::class.fullName,
    data: Data = Data.EMPTY,
    initialDelay: Duration = Duration.ZERO,
    constraints: Constraints = Constraints.EMPTY,
    tags: TagsRequest = TagsRequest(),
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