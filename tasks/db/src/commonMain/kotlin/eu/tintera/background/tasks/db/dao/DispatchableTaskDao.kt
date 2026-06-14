package eu.tintera.background.tasks.db.dao

import androidx.room3.Dao
import androidx.room3.Query
import eu.tintera.background.tasks.db.StateDb
import eu.tintera.background.tasks.db.entities.DispatchableTaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DispatchableTaskDao {
    @Query("SELECT id, state FROM Task WHERE state IN (:states)")
    fun getDispatchableTasksByStates(states: List<StateDb>): Flow<List<DispatchableTaskEntity>>
}