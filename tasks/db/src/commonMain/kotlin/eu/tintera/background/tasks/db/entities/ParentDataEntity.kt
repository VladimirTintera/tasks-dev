package eu.tintera.background.tasks.db.entities

import kotlin.time.Instant
import kotlin.uuid.Uuid

data class ParentDataEntity(
    val id: Uuid,
    val identifier: String,
    val outputData: ByteArray?,
    val finishedAt: Instant?,
    val version: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ParentDataEntity

        if (version != other.version) return false
        if (id != other.id) return false
        if (identifier != other.identifier) return false
        if (!outputData.contentEquals(other.outputData)) return false
        if (finishedAt != other.finishedAt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = version
        result = 31 * result + id.hashCode()
        result = 31 * result + identifier.hashCode()
        result = 31 * result + (outputData?.contentHashCode() ?: 0)
        result = 31 * result + (finishedAt?.hashCode() ?: 0)
        return result
    }
}