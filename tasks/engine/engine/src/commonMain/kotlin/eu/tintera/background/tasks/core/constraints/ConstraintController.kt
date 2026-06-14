package eu.tintera.background.tasks.core.constraints

import eu.tintera.background.tasks.core.ProcessableTask
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.*

internal class ConstraintController(
    constraints: List<Constraint>
) {

    private val allConstraints = constraints.map {
        ReactiveConstraint(it)
    }

    private val executionConstraints = allConstraints.filter {
        it.monitorDuringExecution
    }

    suspend fun waitForAll(
        taskFlow: StateFlow<ProcessableTask?>
    ): Boolean = if (allConstraints.isEmpty()) true
    else combine(
        allConstraints.map {
            it.isValid(taskFlow).onStart { emit(ConstraintResult.Unmet) }
        }
    ) { array ->
        when {
            ConstraintResult.Failed in array -> false
            array.all { it == ConstraintResult.Met } -> true
            else -> null
        }
    }.filterNotNull().first()


    suspend fun waitForUnmet(
        taskFlow: StateFlow<ProcessableTask?>
    ) = if (executionConstraints.isEmpty()) awaitCancellation() else executionConstraints.map {
        it.isValid(taskFlow)
    }.merge().first {
        it != ConstraintResult.Met
    }
}