package eu.tintera.background.tasks.core.constraints

import eu.tintera.background.tasks.core.ProcessableTask
import kotlinx.coroutines.flow.Flow

interface Constraint {
    fun hasConstraint(task: ProcessableTask): Boolean
    fun isValid(task: ProcessableTask): Flow<ConstraintResult>

    val monitorDuringExecution: Boolean
}