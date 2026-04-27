package eu.tintera.tasks.db.dao

import androidx.room.Dao
import androidx.room.Query
import eu.tintera.tasks.db.State
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Dao
interface TaskResultDao {
    @Query(
        """
    WITH RECURSIVE Descendants(id) AS (
        -- 1. Základní případ: Začínáme od kořene (task, který rušíme)
        SELECT :taskId
        UNION ALL
        -- 2. Rekurzivní krok: Najdi všechny děti úkolů, které už máme v Descendants
        SELECT tpt.taskId 
        FROM TaskParentTask tpt
        INNER JOIN Descendants d ON tpt.parentTaskId = d.id
    )
    -- 3. Hromadný Update: Zruš všechny tasky, jejichž ID jsme našli v rekurzi
    UPDATE Task 
    SET state = :state, finishedAt = :finishedAt, processTime = NULL, progressData = NULL
    WHERE id IN Descendants
"""
    )
    suspend fun finishTaskWithUnsuccess(
        taskId: Uuid,
        state: State,
        finishedAt: Instant
    )

    @Query("UPDATE Task set state = :state, finishedAt = :finishedAt, outputData = :outputData, processTime = NULL, progressData = null  WHERE id = :id")
    suspend fun finishTaskWithSuccess(
        id: Uuid,
        state: State,
        finishedAt: Instant,
        outputData: ByteArray
    )

    @Query("UPDATE Task set state = :state, processTime = :processTime, progressData = null, runAttemptCount = 0 WHERE id = :id")
    suspend fun scheduleNextFromBeginning(
        id: Uuid,
        processTime: Instant,
        state: State
    )

    @Query("UPDATE Task set state = :state, processTime = :processTime, progressData = null WHERE id = :id")
    suspend fun scheduleNext(
        id: Uuid,
        processTime: Instant,
        state: State
    )
}