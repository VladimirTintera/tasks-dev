package eu.tintera.tasks.ios

import eu.tintera.tasks.State
import kotlin.time.Instant
import kotlin.uuid.Uuid

interface BgTaskManagerRepository {
    suspend fun tasks(states: List<State>): List<BgTaskManagerTask>
}

data class BgTaskManagerTask(
    val id: Uuid,
    val processTime: Instant,
    val requiresDeviceIdle: Boolean,
    val networkRequired: Boolean,
)