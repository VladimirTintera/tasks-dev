package eu.tintera.tasks.db.dao

import androidx.room.Dao
import androidx.room.Query
import eu.tintera.tasks.db.State
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Dao

interface SchedulableTaskDao {

    @Query("SELECT id, processTime, requiresDeviceIdle, networkRequired FROM Task WHERE state IN (:states)")
    suspend fun schedulableTasks(states: List<State>) : List<SchedulableTask>

    data class SchedulableTask(
        val id: Uuid,
        val processTime: Instant?,
        val requiresDeviceIdle: Boolean,
        val networkRequired: Boolean,
    )
}