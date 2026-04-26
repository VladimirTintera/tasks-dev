package eu.tintera.tasks.core.constraints

import eu.tintera.tasks.core.ProcessableTask
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow

internal class InitialDelayConstraint : Constraint {
    override fun hasConstraint(task: ProcessableTask) = task.initialDelay.isPositive() && task.runAttemptCount == 0

    override fun isValid(task: ProcessableTask) = flow {
        delay(task.initialDelay)
        emit(PreconditionResult.Met)
    }

    override val monitorDuringExecution = false
}