package eu.tintera.background.tasks.db.dao

import androidx.room3.Dao
import androidx.room3.Query
import eu.tintera.background.tasks.db.StateDb
import kotlinx.coroutines.flow.Flow
import kotlin.uuid.Uuid

@Dao
interface ParentConstraintDao {
    @Query("SELECT DISTINCT t.state FROM Task t JOIN TaskParentTask p ON p.parentTaskId = t.id WHERE p.taskId = :id")
    fun parentStatesForTask(id: Uuid): Flow<List<StateDb>>
}