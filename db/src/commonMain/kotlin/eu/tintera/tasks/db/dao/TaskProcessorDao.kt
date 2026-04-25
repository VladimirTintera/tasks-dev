package eu.tintera.tasks.db.dao

import androidx.room.Dao
import androidx.room.Query
import eu.tintera.tasks.db.State
import eu.tintera.tasks.db.entities.GetExecutableTaskByIdTag
import eu.tintera.tasks.db.entities.GetExecutableTasksById
import eu.tintera.tasks.db.entities.ProcessableTaskEntity
import kotlinx.coroutines.flow.Flow
import kotlin.uuid.Uuid

@Dao
interface TaskProcessorDao {
    @Query("SELECT state, initialDelay, runAttemptCount, networkRequired, requiresDeviceIdle, repeatInterval, backoffCriteria, processTime from Task where id = :id")
    fun processableTask(id: Uuid): Flow<ProcessableTaskEntity?>

    @Query("SELECT t.identifier, t.runAttemptCount, t.version, t.inputData, t.outputData, t.progressData, tt.taskId, tt.name from Task t LEFT JOIN TaskTag tt ON tt.taskId = t.id where t.id = :id")
    suspend fun getExecutableTasksById(id: Uuid): Map<GetExecutableTasksById, List<GetExecutableTaskByIdTag>>?

    @Query(
        """
        UPDATE Task set 
            state = :state, 
            processTime = null,
            runAttemptCount = (CASE WHEN :runAttemptCount IS NULL THEN runAttemptCount ELSE :runAttemptCount END)
        WHERE id = :id AND state IN (:allowedSourceStates)"""
    )
    suspend fun updateRunningState(
        id: Uuid,
        state: State,
        allowedSourceStates: List<State>,
        runAttemptCount: Int?
    )

    @Query(
        """
        UPDATE Task set 
            state = :state, 
            processTime = null
        WHERE id = :id AND state IN (:allowedSourceStates)"""
    )
    suspend fun updateEnqueuedState(
        id: Uuid,
        state: State,
        allowedSourceStates: List<State>
    )
}