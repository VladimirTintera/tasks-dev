package eu.tintera.tasks.db.entities

import eu.tintera.tasks.db.StateDb
import kotlin.time.Instant
import kotlin.uuid.Uuid

data class InfoEntity(
    val id: Uuid,
    val identifier: String,
    val runAttemptCount: Int,
    val state: StateDb,
    val outputData: ByteArray?,
    val progressData: ByteArray?,
    val processTime: Instant?,
    val finishedAt: Instant?,
    val createdAt: Instant?,
    val version: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as InfoEntity

        if (runAttemptCount != other.runAttemptCount) return false
        if (version != other.version) return false
        if (id != other.id) return false
        if (identifier != other.identifier) return false
        if (state != other.state) return false
        if (!outputData.contentEquals(other.outputData)) return false
        if (!progressData.contentEquals(other.progressData)) return false
        if (processTime != other.processTime) return false
        if (finishedAt != other.finishedAt) return false
        if (createdAt != other.createdAt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = runAttemptCount
        result = 31 * result + version
        result = 31 * result + id.hashCode()
        result = 31 * result + identifier.hashCode()
        result = 31 * result + state.hashCode()
        result = 31 * result + (outputData?.contentHashCode() ?: 0)
        result = 31 * result + (progressData?.contentHashCode() ?: 0)
        result = 31 * result + (processTime?.hashCode() ?: 0)
        result = 31 * result + (finishedAt?.hashCode() ?: 0)
        result = 31 * result + (createdAt?.hashCode() ?: 0)
        return result
    }
}