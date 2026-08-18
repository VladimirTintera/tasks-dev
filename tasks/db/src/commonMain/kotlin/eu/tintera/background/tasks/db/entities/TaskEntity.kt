package eu.tintera.background.tasks.db.entities

import androidx.room3.*
import eu.tintera.background.tasks.db.BackoffCriteriaDb
import eu.tintera.background.tasks.db.StateDb
import kotlin.time.Duration
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Entity(
    tableName = "Task",
    indices = [
        Index(value = ["state", "processTime"]), // makes the dispatcher query dramatically faster
        Index(value = ["uniqueName"])            // speeds up allByUniqueName
    ]
)
data class TaskEntity(
    @PrimaryKey
    val id: Uuid,
    val identifier: String,
    val uniqueName: String,
    val runAttemptCount: Int,
    val initialDelay: Duration,
    val processTime: Instant?,
    val state: StateDb,
    val inputData: ByteArray?,
    val outputData: ByteArray?,
    val networkRequired: Boolean,
    val createdAt: Instant,
    val finishedAt: Instant?,
    val repeatInterval: Duration?,
    val backoffCriteria: BackoffCriteriaDb?,
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

