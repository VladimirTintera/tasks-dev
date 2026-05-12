package eu.tintera.tasks.db.entities

import androidx.room3.ColumnInfo
import androidx.room3.Dao
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import kotlin.uuid.Uuid

@Entity(
    tableName = "TaskParentTask",
    primaryKeys = ["taskId", "parentTaskId"],
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentTaskId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class TaskParentTaskEntity(
    @ColumnInfo("taskId", index = true)
    val taskId: Uuid,
    @ColumnInfo("parentTaskId", index = true)
    val parentTaskId: Uuid,
)

@Dao
interface TaskParentTaskDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(taskParentTask: TaskParentTaskEntity)

    @Query("SELECT taskId FROM TaskParentTask WHERE parentTaskId = :id")
    suspend fun childrenForTask(id: Uuid): List<Uuid>
}