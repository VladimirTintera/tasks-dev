package eu.tintera.tasks.core.data

import eu.tintera.tasks.BackoffCriteria
import eu.tintera.tasks.State
import eu.tintera.tasks.core.DEFAULT
import kotlin.time.Duration
import kotlin.time.Instant
import kotlin.uuid.Uuid

data class Task(
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
    val retentionDelay: Duration,
    val requiresDeviceIdle: Boolean,
    val version: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Task

        if (runAttemptCount != other.runAttemptCount) return false
        if (networkRequired != other.networkRequired) return false
        if (requiresDeviceIdle != other.requiresDeviceIdle) return false
        if (version != other.version) return false
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
        result = 31 * result + version
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

internal val Task.backoffCriteriaOrDefault: BackoffCriteria get() = backoffCriteria ?: BackoffCriteria.DEFAULT