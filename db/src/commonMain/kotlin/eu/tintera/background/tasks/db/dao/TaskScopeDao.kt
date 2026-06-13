package eu.tintera.background.tasks.db.dao

import androidx.room3.Dao
import androidx.room3.Query
import kotlin.uuid.Uuid

@Dao
interface TaskScopeDao {
    @Query("UPDATE Task set progressData = :progressData WHERE id = :id")
    suspend fun updateProgressData(id: Uuid, progressData: ByteArray?)
}