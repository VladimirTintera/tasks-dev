package eu.tintera.tasks.db.entities

import eu.tintera.tasks.db.BackoffCriteria
import kotlin.time.Duration

data class GetExecutableTasksById(
    val identifier: String,
    val runAttemptCount: Int,
    val version: Int,
    val inputData: ByteArray?,
    val outputData: ByteArray?,
    val progressData: ByteArray?,
    val repeatInterval: Duration?,
    val backoffCriteria: BackoffCriteria?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as GetExecutableTasksById

        if (runAttemptCount != other.runAttemptCount) return false
        if (version != other.version) return false
        if (identifier != other.identifier) return false
        if (!inputData.contentEquals(other.inputData)) return false
        if (!outputData.contentEquals(other.outputData)) return false
        if (!progressData.contentEquals(other.progressData)) return false
        if (repeatInterval != other.repeatInterval) return false
        if (backoffCriteria != other.backoffCriteria) return false

        return true
    }

    override fun hashCode(): Int {
        var result = runAttemptCount
        result = 31 * result + version
        result = 31 * result + identifier.hashCode()
        result = 31 * result + (inputData?.contentHashCode() ?: 0)
        result = 31 * result + (outputData?.contentHashCode() ?: 0)
        result = 31 * result + (progressData?.contentHashCode() ?: 0)
        result = 31 * result + (repeatInterval?.hashCode() ?: 0)
        result = 31 * result + (backoffCriteria?.hashCode() ?: 0)
        return result
    }
}