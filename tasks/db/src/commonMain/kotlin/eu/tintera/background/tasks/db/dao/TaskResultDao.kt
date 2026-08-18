package eu.tintera.background.tasks.db.dao

import androidx.room3.Dao
import androidx.room3.Query
import eu.tintera.background.tasks.db.StateDb
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Dao
interface TaskResultDao {
    @Query(
        """
    WITH RECURSIVE Descendants(id) AS (
        -- 1. Base case: start from the root (the task being cancelled)
        SELECT :taskId
        UNION ALL
        -- 2. Recursive step: find the children of everything already in Descendants
        SELECT tpt.taskId 
        FROM TaskParentTask tpt
        INNER JOIN Descendants d ON tpt.parentTaskId = d.id
    )
    -- 3. Bulk update: cancel every task the recursion found
    UPDATE Task 
    SET state = :state, finishedAt = :finishedAt, processTime = NULL, progressData = NULL
    WHERE id IN Descendants AND state IN (:allowedSourceStates)
"""
    )
    suspend fun finishTaskWithUnsuccess(
        taskId: Uuid,
        state: StateDb,
        allowedSourceStates: List<StateDb>,
        finishedAt: Instant
    )

    @Query("UPDATE Task set state = :state, finishedAt = :finishedAt, outputData = :outputData, processTime = NULL, progressData = null  WHERE id = :id AND state IN (:allowedSourceStates)")
    suspend fun finishTaskWithSuccess(
        id: Uuid,
        state: StateDb,
        allowedSourceStates: List<StateDb>,
        finishedAt: Instant,
        outputData: ByteArray
    )

    @Query("UPDATE Task set state = :state, processTime = :processTime, progressData = null, runAttemptCount = 0 WHERE id = :id AND state IN (:allowedSourceStates)")
    suspend fun scheduleNextFromBeginning(
        id: Uuid,
        processTime: Instant,
        state: StateDb,
        allowedSourceStates: List<StateDb>
    )

    @Query("UPDATE Task set state = :state, processTime = :processTime, progressData = null WHERE id = :id AND state IN (:allowedSourceStates) ")
    suspend fun scheduleNext(
        id: Uuid,
        processTime: Instant,
        state: StateDb,
        allowedSourceStates: List<StateDb>
    )
}