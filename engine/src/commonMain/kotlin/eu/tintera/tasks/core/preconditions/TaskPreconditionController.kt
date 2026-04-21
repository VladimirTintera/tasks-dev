package eu.tintera.tasks.core.preconditions

import eu.tintera.tasks.core.data.ProcessableTask
import eu.tintera.tasks.core.data.Task
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge

internal class TaskPreconditionController(
    preconditions: List<TaskPrecondition>
) {

    enum class WaitResult {
        SUCCESS,
        FAILED,
        CANCELED
    }

    private val allPreconditions = preconditions.map {
        ReactiveTaskPrecondition(it)
    }

    private val executionPreconditions = allPreconditions.filter {
        it.monitorDuringExecution
    }

    suspend fun waitForAll(
        taskFlow: StateFlow<ProcessableTask?>
    ): WaitResult = if (allPreconditions.isEmpty()) WaitResult.SUCCESS
    else combine(allPreconditions.map { it.isValid(taskFlow) }) { array ->
        when {
            PreconditionResult.Failed in array -> WaitResult.FAILED
            PreconditionResult.Cancelled in array -> WaitResult.CANCELED
            array.all { it == PreconditionResult.Met } -> WaitResult.SUCCESS
            else -> null
        }
    }.filterNotNull().first()


    suspend fun waitForUnmet(
        taskFlow: StateFlow<ProcessableTask?>
    ) = if (executionPreconditions.isEmpty()) awaitCancellation() else executionPreconditions.map {
        it.isValid(taskFlow)
    }.merge().first {
        it != PreconditionResult.Met
    }
}