package eu.tintera.tasks.db.entities

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import eu.tintera.tasks.db.BackoffCriteria
import eu.tintera.tasks.db.SerializableTaskData
import eu.tintera.tasks.db.State
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant
import kotlin.time.Duration
import kotlin.uuid.Uuid

@Entity(
    tableName = "Task",
    indices = [
        Index(value = ["state", "processTime"]), // Extrémně zrychlí Dispatcher!
        Index(value = ["uniqueName"])            // Zrychlí allByUniqueName
    ]
)
internal data class TaskEntity(
    @PrimaryKey
    val id: Uuid,
    val identifier: String,
    val uniqueName: String,
    val runAttemptCount: Int,
    val initialDelay: Duration,
    val processTime: Instant,
    val state: State,
    val inputData: SerializableTaskData,
    val outputData: SerializableTaskData,
    val networkRequired: Boolean,
    val createdAt: Instant,
    val finishedAt: Instant?,
    val repeatInterval: Duration?,
    val backoffCriteria: BackoffCriteria?,
    val progressData: SerializableTaskData?,
    @ColumnInfo(defaultValue = "86400000")
    val retentionDelay: Duration,
    @ColumnInfo(defaultValue = "0")
    val requiresDeviceIdle: Boolean
)

@Dao
internal interface TaskDao {

    @Query("UPDATE Task set state = :state, processTime = :processTime, progressData = :progress WHERE id = :id")
    suspend fun updateRetry(id: Uuid, processTime: Instant, state: State, progress: SerializableTaskData)

    @Query("UPDATE Task set runAttemptCount = :runAttemptCount WHERE id = :id")
    suspend fun updateRunAttemptCount(id: Uuid, runAttemptCount: Int)

    @Query("UPDATE Task set progressData = :progressData WHERE id = :id")
    suspend fun updateProgressData(id: Uuid, progressData: SerializableTaskData)

    @Query("UPDATE Task set state = :state WHERE id = :id AND state IN (:allowedSourceStates)")
    suspend fun updateState(id: Uuid, state: State, allowedSourceStates: List<State>)

    @Query("UPDATE Task set state = :state, finishedAt = :finishedAt, outputData = :outputData WHERE id = :id")
    suspend fun updateTerminatingState(
        id: Uuid,
        state: State,
        finishedAt: Instant,
        outputData: SerializableTaskData,
    )

    @Query("SELECT * FROM Task WHERE state IN (:states)")
    fun tasksByState(states: List<State>): Flow<List<TaskEntity>>

    @Query("SELECT * FROM Task JOIN TaskTag ON TaskTag.taskId = Task.id WHERE Task.id = :id")
    fun taskInfoById(id: Uuid): Flow<Map<TaskEntity, List<TaskTag>>>

    @Query("SELECT * FROM Task LEFT JOIN TaskTag ON TaskTag.taskId = Task.id WHERE EXISTS(SELECT 1 FROM TaskTag tag WHERE tag.name = :name AND tag.taskId = Task.id)")
    fun taskInfoByTag(name: String): Flow<Map<TaskEntity, List<TaskTag>>>


    @Query(
        "SELECT * FROM Task WHERE state IN (:states) AND EXISTS(SELECT * FROM TaskTag WHERE TaskTag.taskId = Task.id AND TaskTag.name = :tag)"
    )
    suspend fun tasksByTagAndState(states: List<State>, tag: String): List<TaskEntity>

    @Query("SELECT * FROM Task WHERE id = :id")
    fun task(id: Uuid): Flow<TaskEntity?>

    @Query("SELECT state FROM Task WHERE id = :id")
    suspend fun taskState(id: Uuid): State?

    @Query("SELECT t.* FROM Task t JOIN TaskParentTask p ON p.parentTaskId = t.id WHERE p.taskId = :id")
    fun parentsFor(id: Uuid): Flow<List<TaskEntity>>

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
            AND (CAST(strftime('%s', finishedAt) AS INTEGER) * 1000 + retentionDelay) <= :currentTimeMillis"""
    )
    suspend fun cleanOld(currentTimeMillis: Long, states: List<State>)

    @Query("UPDATE Task set state = :to WHERE state = :from")
    suspend fun resetState(from: State, to: State)

    @Query("UPDATE Task set state = :to WHERE state = :from AND id NOT IN (:excludedIds)")
    suspend fun resetStateWithExclusion(from: State, to: State, excludedIds: Set<Uuid>)

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
    SET state = :state 
    WHERE id IN Descendants 
    AND state IN (:allowedSourceStates)
"""
    )
    suspend fun updateStateTaskAndAllDescendants(
        taskId: Uuid,
        state: State,
        allowedSourceStates: List<State>
    )
}