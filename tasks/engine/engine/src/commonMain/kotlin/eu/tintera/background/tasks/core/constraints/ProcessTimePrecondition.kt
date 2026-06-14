package eu.tintera.background.tasks.core.constraints

import eu.tintera.background.tasks.core.ProcessableTask
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlin.time.Clock

internal class ProcessTimePrecondition : Constraint {

    override fun hasConstraint(task: ProcessableTask) = task.processTime != null

    override fun isValid(task: ProcessableTask) = flow {
        task.processTime?.also {
            val now = Clock.System.now()
            val diff = it - now
            if (diff.isPositive()) delay(diff)
        }
        emit(ConstraintResult.Met)
    }

    override val monitorDuringExecution = false
}