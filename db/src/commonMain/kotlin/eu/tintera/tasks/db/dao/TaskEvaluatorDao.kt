package eu.tintera.tasks.db.dao

import androidx.room.Dao
import androidx.room.Query
import eu.tintera.tasks.db.entities.ParentDataEntity
import kotlin.uuid.Uuid

@Dao
internal interface TaskEvaluatorDao {

    @Query("SELECT t.id, t.identifier, t.outputData, t.finishedAt, t.version FROM Task t JOIN TaskParentTask p ON p.parentTaskId = t.id WHERE p.taskId = :id")
    suspend fun parentsDataFor(id: Uuid): List<ParentDataEntity>

    @Query("UPDATE Task set version = :version, inputData = :input, outputData = :output, progressData = :progress WHERE id = :taskId")
    suspend fun upgradeData(
        taskId: Uuid,
        input: ByteArray?,
        output: ByteArray?,
        progress: ByteArray?,
        version: Int
    )
}