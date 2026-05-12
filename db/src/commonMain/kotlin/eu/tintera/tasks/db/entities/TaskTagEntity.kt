package eu.tintera.tasks.db.entities

import androidx.room3.*
import kotlin.uuid.Uuid

@Entity(
    tableName = "TaskTag",
    primaryKeys = ["taskId", "name"],
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class TaskTagEntity(
    val taskId: Uuid,
    val name: String,
)

@Dao
interface TaskTagDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(taskTags: List<TaskTagEntity>)
}