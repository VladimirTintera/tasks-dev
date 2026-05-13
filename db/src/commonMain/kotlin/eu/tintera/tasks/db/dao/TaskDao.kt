package eu.tintera.tasks.db.dao

import androidx.room3.*
import eu.tintera.tasks.db.StateDb
import eu.tintera.tasks.db.entities.InfoEntity
import eu.tintera.tasks.db.entities.TaskEntity
import eu.tintera.tasks.db.entities.TaskTagEntity
import eu.tintera.tasks.db.entities.TaskWithTagsEntity
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Dao
interface TaskDao {

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
        state: StateDb,
        allowedSourceStates: List<StateDb>,
        resetProcessTime: Boolean,
        runAttemptCount: Int?
    )

    @Query("UPDATE Task set state = :state, finishedAt = :finishedAt, outputData = :outputData, processTime = NULL, progressData = null  WHERE id = :id")
    suspend fun updateTerminatingState(
        id: Uuid,
        state: StateDb,
        finishedAt: Instant,
        outputData: ByteArray?
    )

    @Query("SELECT id, identifier, runAttemptCount, state, outputData, processTime, progressData, finishedAt, createdAt, version FROM Task WHERE id = :id")
    fun taskInfoById(id: Uuid): Flow<TaskWithTagsEntity?>

    @Query("SELECT id, identifier, runAttemptCount, state, outputData, processTime, progressData, finishedAt, createdAt, version FROM Task WHERE id IN (:ids)")
    fun taskInfoByIds(ids: Set<Uuid>): Flow<List<InfoEntity>>

    @Transaction // Nutné pro @Relation dotazy
    @Query(
        """
    SELECT id, identifier, runAttemptCount, state, outputData, processTime, progressData, finishedAt, createdAt, version FROM Task 
    WHERE EXISTS(SELECT 1 FROM TaskTag tag WHERE tag.name = :name AND tag.taskId = Task.id)
"""
    )
    fun taskInfoByTag(name: String): Flow<List<TaskWithTagsEntity>>

    @RawQuery(observedEntities = [TaskEntity::class, TaskTagEntity::class])
    fun taskInfoByRawQuery(query: RoomRawQuery): Flow<Map<InfoEntity, List<TaskTagEntity>>>

    @Query(
        "SELECT id FROM Task WHERE state IN (:states) AND EXISTS(SELECT * FROM TaskTag WHERE TaskTag.taskId = Task.id AND TaskTag.name = :tag)"
    )
    suspend fun taskIdsByTagAndState(states: List<StateDb>, tag: String): List<Uuid>

    @Query("SELECT * FROM Task WHERE id = :id")
    suspend fun task(id: Uuid): TaskEntity?

    @Query("DELETE FROM Task WHERE id = :id")
    suspend fun delete(id: Uuid)

    @Query("SELECT id FROM Task WHERE uniqueName = :uniqueName AND state IN (:states)")
    suspend fun allByUniqueName(uniqueName: String, states: List<StateDb>): List<Uuid>

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
    suspend fun deleteByStateUniqueNameWithoutChildren(uniqueName: String, states: List<StateDb>)

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
    suspend fun cleanOld(currentTimeMillis: Long, states: List<StateDb>)


}