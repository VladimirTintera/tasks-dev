package eu.tintera.background.tasks.core.constraints

import eu.tintera.background.tasks.State
import eu.tintera.background.tasks.core.ProcessableTask
import eu.tintera.background.tasks.core.failedStates
import eu.tintera.background.tasks.core.terminal
import eu.tintera.background.tasks.core.terminalStates
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformWhile

internal class ParentsConstraint(
    private val repository: ParentsConstraintRepository
) : Constraint {

    override fun hasConstraint(task: ProcessableTask): Boolean = true

    override fun isValid(
        task: ProcessableTask
    ) = repository.parentStates(task.id).map { states ->
        when {
            states.isEmpty() -> ConstraintResult.Met
            states.any { it in failedStates } -> ConstraintResult.Failed
            states.all { it.terminal() } -> ConstraintResult.Met
            else -> ConstraintResult.Unmet
        }
    }.distinctUntilChanged().transformWhile {
        emit(it)
        it == ConstraintResult.Unmet
    }

    override val monitorDuringExecution: Boolean = false
}