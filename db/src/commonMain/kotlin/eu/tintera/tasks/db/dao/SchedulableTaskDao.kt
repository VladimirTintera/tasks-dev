package eu.tintera.tasks.db.dao

import androidx.room3.Dao
import androidx.room3.Query
import eu.tintera.tasks.db.StateDb
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Dao

interface SchedulableTaskDao {

    @Query("SELECT id, processTime, requiresDeviceIdle, networkRequired FROM Task WHERE state IN (:states)")
    suspend fun schedulableTasks(states: List<StateDb>) : List<SchedulableTask>

    data class SchedulableTask(
        val id: Uuid,
        val processTime: Instant?,
        val requiresDeviceIdle: Boolean,
        val networkRequired: Boolean,
    )
}