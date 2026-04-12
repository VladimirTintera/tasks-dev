package eu.tintera.tasks.core.preconditions

import eu.tintera.tasks.core.TaskPrecondition
import eu.tintera.tasks.core.data.Task
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

internal class TaskPreconditionController(
    private val preconditions: List<TaskPrecondition>
) {

    private val executionPreconditions = preconditions.filter { it.monitorDuringExecution }

    suspend fun waitForAll(task: Task) = preconditions.filter {
        it.hasConstraint(task)
    }.takeIf { it.isNotEmpty() }?.also { preconditions ->
        combine(preconditions.map { it.isValid(task) }) { array ->
            array.all { it }
        }.first { it }
    }

    suspend fun waitForFail(task: Task): List<TaskPrecondition> {
        val taskPreconditions = executionPreconditions.filter { it.hasConstraint(task) }

        if (taskPreconditions.isEmpty()) awaitCancellation()

        return combine(
            taskPreconditions.map { precondition ->
                precondition.isValid(task).map { isValid -> precondition to isValid }
            }
        ) { array ->
            array.filter { !it.second }.map { it.first }.takeIf { it.isNotEmpty() }
        }.filterNotNull().first()
    }
}