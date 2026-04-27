package eu.tintera.tasks.core.constraints

import eu.tintera.tasks.core.ProcessableTask
import kotlinx.coroutines.flow.Flow

interface Constraint {
    fun hasConstraint(task: ProcessableTask): Boolean
    fun isValid(task: ProcessableTask): Flow<ConstraintResult>

    val monitorDuringExecution: Boolean
}