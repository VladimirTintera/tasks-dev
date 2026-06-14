package eu.tintera.background.tasks.core

import eu.tintera.background.tasks.State
import kotlin.time.Instant
import kotlin.uuid.Uuid

interface DispatchableTasksRepository {
    suspend fun dispatchableTasks(states: List<State>): List<DispatchableTask>
}

data class DispatchableTask(
    val id: Uuid,
    val processTime: Instant,
)