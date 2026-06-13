package eu.tintera.background.tasks.db.dao

import androidx.room3.Dao
import androidx.room3.Query
import eu.tintera.background.tasks.db.StateDb
import eu.tintera.background.tasks.db.entities.ProcessableTaskEntity
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Dao
interface TaskProcessorDao {
    @Query("SELECT state, initialDelay, runAttemptCount, networkRequired, requiresDeviceIdle, repeatInterval, backoffCriteria, processTime from Task where id = :id")
    fun processableTask(id: Uuid): Flow<ProcessableTaskEntity?>

    @Query(
        """
        UPDATE Task set 
            state = :state, 
            processTime = null,
            runAttemptCount = runAttemptCount + 1
        WHERE id = :id AND state IN (:allowedSourceStates)"""
    )
    suspend fun run(
        id: Uuid,
        state: StateDb,
        allowedSourceStates: List<StateDb>,
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
        state: StateDb,
        allowedSourceStates: List<StateDb>
    )

    @Query("UPDATE Task set state = :state, processTime = :processTime, progressData = null WHERE id = :id AND state IN (:allowedSourceStates) ")
    suspend fun enqueue(
        id: Uuid,
        processTime: Instant,
        state: StateDb,
        allowedSourceStates: List<StateDb>
    )

    @Query("UPDATE Task set state = :state, processTime = null, progressData = null, outputData = null WHERE id = :id")
    suspend fun fail(
        id: Uuid,
        state: StateDb
    )
}