package eu.tintera.tasks.core.preconditions

import eu.tintera.tasks.core.data.ProcessableTask
import eu.tintera.tasks.core.data.Task
import kotlinx.coroutines.flow.Flow

interface TaskPrecondition {
    fun hasConstraint(task: ProcessableTask) : Boolean
    fun isValid(task: ProcessableTask) : Flow<PreconditionResult>

    val monitorDuringExecution: Boolean
}