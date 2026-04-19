package eu.tintera.tasks.core.preconditions

import eu.tintera.tasks.core.data.Task
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

internal class TaskPreconditionController(
    private val preconditions: List<TaskPrecondition>
) {

    enum class WaitResult {
        SUCCESS,
        FAILED,
        CANCELED
    }

    private val executionPreconditions = preconditions.filter { it.monitorDuringExecution }

    suspend fun waitForAll(task: Task): WaitResult = preconditions.filter {
        it.hasConstraint(task)
    }.takeIf { it.isNotEmpty() }?.let { preconditions ->
        combine(preconditions.map { it.isValid(task) }) { array ->
            when {
                PreconditionResult.Failed in array -> WaitResult.FAILED
                PreconditionResult.Cancelled in array -> WaitResult.CANCELED
                array.all { it == PreconditionResult.Met } -> WaitResult.SUCCESS
                else -> null
            }
        }.first {
            it != null
        }
    } ?: WaitResult.SUCCESS

    suspend fun waitForUnmet(task: Task): List<TaskPrecondition> {
        val taskPreconditions = executionPreconditions.filter { it.hasConstraint(task) }

        if (taskPreconditions.isEmpty()) awaitCancellation()

        return combine(
            taskPreconditions.map { precondition ->
                precondition.isValid(task).map { isValid -> precondition to isValid }
            }
        ) { array ->
            array.filter { it.second != PreconditionResult.Met }.map { it.first }.takeIf { it.isNotEmpty() }
        }.filterNotNull().first()
    }
}