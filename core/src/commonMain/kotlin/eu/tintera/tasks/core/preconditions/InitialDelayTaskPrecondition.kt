package eu.tintera.tasks.core.preconditions

import eu.tintera.tasks.core.TaskPrecondition
import eu.tintera.tasks.core.data.Task
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class InitialDelayTaskPrecondition : TaskPrecondition {
    override fun hasConstraint(task: Task) = task.initialDelay.isPositive()

    override fun isValid(task: Task): Flow<Boolean> = flow {
        delay(task.initialDelay)
        emit(true)
    }

    override val monitorDuringExecution = false
}