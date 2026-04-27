package eu.tintera.tasks.core.constraints

import eu.tintera.tasks.core.ProcessableTask
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.*

internal class ConstraintController(
    preconditions: List<Constraint>
) {

    private val allPreconditions = preconditions.map {
        ReactiveConstraint(it)
    }

    private val executionPreconditions = allPreconditions.filter {
        it.monitorDuringExecution
    }

    suspend fun waitForAll(
        taskFlow: StateFlow<ProcessableTask?>
    ): Boolean = if (allPreconditions.isEmpty()) true
    else combine(
        allPreconditions.map {
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
    ) = if (executionPreconditions.isEmpty()) awaitCancellation() else executionPreconditions.map {
        it.isValid(taskFlow)
    }.merge().first {
        it != ConstraintResult.Met
    }
}