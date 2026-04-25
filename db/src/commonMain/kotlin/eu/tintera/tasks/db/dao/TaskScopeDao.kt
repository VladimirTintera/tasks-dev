package eu.tintera.tasks.db.dao

import androidx.room.Dao
import androidx.room.Query
import kotlin.uuid.Uuid

@Dao
interface TaskScopeDao {
    @Query("UPDATE Task set progressData = :progressData WHERE id = :id")
    suspend fun updateProgressData(id: Uuid, progressData: ByteArray?)
}