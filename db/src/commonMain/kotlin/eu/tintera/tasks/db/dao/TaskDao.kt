package eu.tintera.tasks.db.dao

import androidx.room.*
import eu.tintera.tasks.db.State
import eu.tintera.tasks.db.entities.InfoEntity
import eu.tintera.tasks.db.entities.TaskEntity
import eu.tintera.tasks.db.entities.TaskTag
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Dao
interface TaskDao {

    @Query("UPDATE Task set state = :state, processTime = :processTime, progressData = :progress, runAttemptCount = (CASE WHEN :runAttemptCount IS NULL THEN runAttemptCount ELSE :runAttemptCount END) WHERE id = :id")
    suspend fun updateRetry(
        id: Uuid,
        processTime: Instant,
        state: State,
        progress: ByteArray?,
        runAttemptCount: Int?
    )

    @Query("UPDATE Task set runAttemptCount = :runAttemptCount WHERE id = :id")
    suspend fun updateRunAttemptCount(id: Uuid, runAttemptCount: Int)


    @Query(
        """
        UPDATE Task set 
            state = :state, 
            processTime = (CASE WHEN :resetProcessTime THEN NULL ELSE processTime END),
            runAttemptCount = (CASE WHEN :runAttemptCount IS NULL THEN runAttemptCount ELSE :runAttemptCount END)
        WHERE id = :id AND state IN (:allowedSourceStates)"""
    )
    suspend fun updateState(
        id: Uuid,
        state: State,
        allowedSourceStates: List<State>,
        resetProcessTime: Boolean,
        runAttemptCount: Int?
    )

    @Query("UPDATE Task set state = :state, finishedAt = :finishedAt, outputData = :outputData, processTime = NULL, progressData = null  WHERE id = :id")
    suspend fun updateTerminatingState(
        id: Uuid,
        state: State,
        finishedAt: Instant,
        outputData: ByteArray?
    )

    @Query("SELECT Task.id, Task.identifier, Task.runAttemptCount, Task.state, Task.outputData, Task.processTime, Task.progressData, Task.finishedAt, Task.createdAt, Task.version, TaskTag.taskId, TaskTag.name FROM Task JOIN TaskTag ON TaskTag.taskId = Task.id WHERE Task.id = :id")
    fun taskInfoById(id: Uuid): Flow<Map<InfoEntity, List<TaskTag>>>

    @Query("SELECT id, identifier, runAttemptCount, state, outputData, processTime, progressData, finishedAt, createdAt, version FROM Task WHERE id IN (:ids)")
    fun taskInfoByIds(ids: Set<Uuid>): Flow<List<InfoEntity>>

    @Query("SELECT Task.id, Task.identifier, Task.runAttemptCount, Task.state, Task.outputData, Task.processTime, Task.progressData, Task.finishedAt, Task.createdAt, Task.version, TaskTag.taskId, TaskTag.name FROM Task LEFT JOIN TaskTag ON TaskTag.taskId = Task.id WHERE EXISTS(SELECT 1 FROM TaskTag tag WHERE tag.name = :name AND tag.taskId = Task.id)")
    fun taskInfoByTag(name: String): Flow<Map<InfoEntity, List<TaskTag>>>

    @RawQuery(observedEntities = [TaskEntity::class, TaskTag::class])
    fun taskInfoByRawQuery(query: RoomRawQuery): Flow<Map<InfoEntity, List<TaskTag>>>

    @Query(
        "SELECT id FROM Task WHERE state IN (:states) AND EXISTS(SELECT * FROM TaskTag WHERE TaskTag.taskId = Task.id AND TaskTag.name = :tag)"
    )
    suspend fun taskIdsByTagAndState(states: List<State>, tag: String): List<Uuid>

    @Query("SELECT * FROM Task WHERE id = :id")
    suspend fun task(id: Uuid): TaskEntity?


    @Query("SELECT DISTINCT t.state FROM Task t JOIN TaskParentTask p ON p.parentTaskId = t.id WHERE p.taskId = :id")
    fun parentStatesForTask(id: Uuid): Flow<List<State>>

    @Query("DELETE FROM Task WHERE id = :id")
    suspend fun delete(id: Uuid)

    @Query("SELECT * FROM Task WHERE uniqueName = :uniqueName")
    suspend fun allByUniqueName(uniqueName: String): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(task: TaskEntity)

    @Query(
        """DELETE FROM Task WHERE uniqueName = :uniqueName AND state IN (:states) AND NOT EXISTS (
                SELECT 1 FROM TaskParentTask pt
                JOIN Task child ON child.id = pt.taskId
                WHERE child.uniqueName = :uniqueName
                AND pt.parentTaskId = Task.id AND child.state NOT IN (:states)
                )"""
    )
    suspend fun deleteByStateUniqueNameWithoutChildren(uniqueName: String, states: List<State>)

    @Query(
        """DELETE FROM Task WHERE
            state IN (:states) 
            AND NOT EXISTS (
                SELECT 1 FROM TaskParentTask pt
                JOIN Task child ON child.id = pt.taskId
                AND pt.parentTaskId = Task.id AND child.state NOT IN (:states)
            )
            AND (finishedAt + retentionDelay) <= :currentTimeMillis"""
    )
    suspend fun cleanOld(currentTimeMillis: Long, states: List<State>)

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
    AND state IN (:allowedSourceStates)
"""
    )
    suspend fun updateTerminatingStateWithAllDescendants(
        taskId: Uuid,
        state: State,
        allowedSourceStates: List<State>,
        finishedAt: Instant
    )
}