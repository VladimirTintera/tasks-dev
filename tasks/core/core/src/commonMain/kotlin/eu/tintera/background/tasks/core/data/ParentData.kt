package eu.tintera.background.tasks.core.data

import eu.tintera.background.tasks.core.migrations.MigratableData
import kotlin.time.Instant
import kotlin.uuid.Uuid

data class ParentData(
    val id: Uuid,
    val identifier: String,
    override val outputData: ByteArray?,
    val finishedAt: Instant,
    override val version: Int
) : MigratableData {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ParentData

        if (id != other.id) return false
        if (identifier != other.identifier) return false
        if (!outputData.contentEquals(other.outputData)) return false
        if (finishedAt != other.finishedAt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + identifier.hashCode()
        result = 31 * result + (outputData?.contentHashCode() ?: 0)
        result = 31 * result + finishedAt.hashCode()
        return result
    }

    override val inputData: ByteArray? = null
    override val progressData: ByteArray? = null
}