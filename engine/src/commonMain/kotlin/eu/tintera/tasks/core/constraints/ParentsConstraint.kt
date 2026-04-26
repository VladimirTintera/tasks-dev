package eu.tintera.tasks.core.constraints

import eu.tintera.tasks.State
import eu.tintera.tasks.core.ProcessableTask
import eu.tintera.tasks.core.terminal
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
            states.isEmpty() -> PreconditionResult.Met
            State.Failed in states -> PreconditionResult.Failed
            State.Cancelled in states -> PreconditionResult.Cancelled
            states.all { it.terminal() } -> PreconditionResult.Met
            else -> PreconditionResult.Unmet
        }
    }.distinctUntilChanged().transformWhile {
        emit(it)
        it == PreconditionResult.Unmet
    }

    override val monitorDuringExecution: Boolean = false
}