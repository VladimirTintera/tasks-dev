package eu.tintera.tasks.core.preconditions

import eu.tintera.tasks.core.data.Task
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlin.time.Clock

internal class ProcessTimePrecondition : TaskPrecondition {

    override fun hasConstraint(task: Task) = task.processTime != null

    override fun isValid(task: Task) = flow {
        task.processTime?.also {
            val now = Clock.System.now()
            val diff = it - now
            if (diff.isPositive()) delay(diff)
        }
        emit(PreconditionResult.Met)
    }

    override val monitorDuringExecution = false
}