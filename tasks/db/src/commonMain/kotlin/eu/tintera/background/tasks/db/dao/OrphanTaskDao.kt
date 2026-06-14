package eu.tintera.background.tasks.db.dao

import androidx.room3.Dao
import androidx.room3.Query
import eu.tintera.background.tasks.db.StateDb
import kotlin.uuid.Uuid

@Dao
interface OrphanTaskDao {

    @Query("UPDATE Task set state = :to, progressData = null WHERE state = :from")
    suspend fun resetState(from: StateDb, to: StateDb)

    @Query("UPDATE Task set state = :to, progressData = null WHERE state = :from AND id NOT IN (:excludedIds)")
    suspend fun resetStateWithExclusion(from: StateDb, to: StateDb, excludedIds: Set<Uuid>)
}