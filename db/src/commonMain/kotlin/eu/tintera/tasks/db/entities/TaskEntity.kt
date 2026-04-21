package eu.tintera.tasks.db.entities

import androidx.room.*
import eu.tintera.tasks.db.BackoffCriteria
import eu.tintera.tasks.db.State
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration
import kotlin.time.Instant
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
    val processTime: Instant?,
    val state: State,
    val inputData: ByteArray?,
    val outputData: ByteArray?,
    val networkRequired: Boolean,
    val createdAt: Instant,
    val finishedAt: Instant?,
    val repeatInterval: Duration?,
    val backoffCriteria: BackoffCriteria?,
    val progressData: ByteArray?,
    @ColumnInfo(defaultValue = "86400000")
    val retentionDelay: Duration,
    @ColumnInfo(defaultValue = "0")
    val requiresDeviceIdle: Boolean,
    @ColumnInfo(defaultValue = "1")
    val version: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as TaskEntity

        if (runAttemptCount != other.runAttemptCount) return false
        if (networkRequired != other.networkRequired) return false
        if (requiresDeviceIdle != other.requiresDeviceIdle) return false
        if (id != other.id) return false
        if (identifier != other.identifier) return false
        if (uniqueName != other.uniqueName) return false
        if (initialDelay != other.initialDelay) return false
        if (processTime != other.processTime) return false
        if (state != other.state) return false
        if (!inputData.contentEquals(other.inputData)) return false
        if (!outputData.contentEquals(other.outputData)) return false
        if (createdAt != other.createdAt) return false
        if (finishedAt != other.finishedAt) return false
        if (repeatInterval != other.repeatInterval) return false
        if (backoffCriteria != other.backoffCriteria) return false
        if (!progressData.contentEquals(other.progressData)) return false
        if (retentionDelay != other.retentionDelay) return false

        return true
    }

    override fun hashCode(): Int {
        var result = runAttemptCount
        result = 31 * result + networkRequired.hashCode()
        result = 31 * result + requiresDeviceIdle.hashCode()
        result = 31 * result + id.hashCode()
        result = 31 * result + identifier.hashCode()
        result = 31 * result + uniqueName.hashCode()
        result = 31 * result + initialDelay.hashCode()
        result = 31 * result + (processTime?.hashCode() ?: 0)
        result = 31 * result + state.hashCode()
        result = 31 * result + (inputData?.contentHashCode() ?: 0)
        result = 31 * result + (outputData?.contentHashCode() ?: 0)
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + (finishedAt?.hashCode() ?: 0)
        result = 31 * result + (repeatInterval?.hashCode() ?: 0)
        result = 31 * result + (backoffCriteria?.hashCode() ?: 0)
        result = 31 * result + (progressData?.contentHashCode() ?: 0)
        result = 31 * result + retentionDelay.hashCode()
        return result
    }
}

@Dao
internal interface TaskDao {

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

    @Query("UPDATE Task set progressData = :progressData WHERE id = :id")
    suspend fun updateProgressData(id: Uuid, progressData: ByteArray?)

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

    @Query("SELECT * FROM Task WHERE state IN (:states)")
    fun tasksByState(states: List<State>): Flow<List<TaskEntity>>

    @Query("SELECT Task.id, Task.identifier, Task.runAttemptCount, Task.state, Task.outputData, Task.processTime, Task.progressData, Task.finishedAt, Task.createdAt, Task.version, TaskTag.taskId, TaskTag.name FROM Task JOIN TaskTag ON TaskTag.taskId = Task.id WHERE Task.id = :id")
    fun taskInfoById(id: Uuid): Flow<Map<InfoEntity, List<TaskTag>>>

    @Query("SELECT id, identifier, runAttemptCount, state, outputData, processTime, progressData, finishedAt, createdAt, version FROM Task WHERE id IN (:ids)")
    fun taskInfoByIds(ids: Set<Uuid>): Flow<List<InfoEntity>>

    @Query("SELECT Task.id, Task.identifier, Task.runAttemptCount, Task.state, Task.outputData, Task.processTime, Task.progressData, Task.finishedAt, Task.createdAt, Task.version, TaskTag.taskId, TaskTag.name FROM Task LEFT JOIN TaskTag ON TaskTag.taskId = Task.id WHERE EXISTS(SELECT 1 FROM TaskTag tag WHERE tag.name = :name AND tag.taskId = Task.id)")
    fun taskInfoByTag(name: String): Flow<Map<InfoEntity, List<TaskTag>>>

    @Query(
        "SELECT id FROM Task WHERE state IN (:states) AND EXISTS(SELECT * FROM TaskTag WHERE TaskTag.taskId = Task.id AND TaskTag.name = :tag)"
    )
    suspend fun taskIdsByTagAndState(states: List<State>, tag: String): List<Uuid>

    @Query("SELECT * FROM Task WHERE id = :id")
    suspend fun task(id: Uuid): TaskEntity?

    @Query("SELECT t.id, t.identifier, t.outputData, t.finishedAt, t.version FROM Task t JOIN TaskParentTask p ON p.parentTaskId = t.id WHERE p.taskId = :id")
    suspend fun parentsDataFor(id: Uuid): List<ParentDataEntity>

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

    @Query("UPDATE Task set version = :version, inputData = :input, outputData = :output, progressData = :progress WHERE id = :taskId")
    suspend fun upgradeData(
        taskId: Uuid,
        input: ByteArray?,
        output: ByteArray?,
        progress: ByteArray?,
        version: Int
    )

    @Query("SELECT id, state FROM Task WHERE state IN (:states)")
    fun dispatchableTasks(states: List<State>): Flow<List<DispatchableTaskEntity>>

    @Query("SELECT state, initialDelay, runAttemptCount, networkRequired, requiresDeviceIdle, repeatInterval, backoffCriteria, processTime from Task where id = :id")
    fun processableTask(id: Uuid): Flow<ProcessableTaskEntity?>

    @Query("SELECT identifier, runAttemptCount, version, inputData, outputData, progressData from Task where id = :id")
    suspend fun executableTask(id: Uuid): ExecutableTaskEntity?

}