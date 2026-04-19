package eu.tintera.tasks.core.preconditions

import eu.tintera.tasks.core.data.Task
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow

internal class InitialDelayTaskPrecondition : TaskPrecondition {
    override fun hasConstraint(task: Task) = task.initialDelay.isPositive() && task.runAttemptCount == 0

    override fun isValid(task: Task) = flow {
        delay(task.initialDelay)
        emit(PreconditionResult.Met)
    }

    override val monitorDuringExecution = false
}