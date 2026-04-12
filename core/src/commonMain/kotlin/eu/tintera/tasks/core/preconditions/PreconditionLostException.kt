package eu.tintera.tasks.core.preconditions

import eu.tintera.tasks.core.TaskPrecondition
import kotlinx.coroutines.CancellationException

internal class PreconditionLostException(
    val failedPreconditions: List<TaskPrecondition>
): CancellationException("Required execution capability was lost during execution: $failedPreconditions")