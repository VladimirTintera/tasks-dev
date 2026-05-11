package eu.tintera.tasks.db.dao

import androidx.room3.Dao
import androidx.room3.Query
import eu.tintera.tasks.db.State
import kotlin.uuid.Uuid

@Dao
interface OrphanTaskDao {

    @Query("UPDATE Task set state = :to, progressData = null WHERE state = :from")
    suspend fun resetState(from: State, to: State)

    @Query("UPDATE Task set state = :to, progressData = null WHERE state = :from AND id NOT IN (:excludedIds)")
    suspend fun resetStateWithExclusion(from: State, to: State, excludedIds: Set<Uuid>)
}