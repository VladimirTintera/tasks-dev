package eu.tintera.background.tasks.core.data

import eu.tintera.background.tasks.BackoffCriteria
import eu.tintera.background.tasks.core.migrations.MigratableData
import kotlin.time.Duration

data class ExecutableTask(
    val identifier: String,
    val runAttemptCount: Int,
    val backoffCriteria: BackoffCriteria?,
    val repeatInterval: Duration?,
    override val version: Int,
    override val inputData: ByteArray?,
    override val outputData: ByteArray?,
    override val progressData: ByteArray?,
    val tags: Set<String>
) : MigratableData {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ExecutableTask

        if (runAttemptCount != other.runAttemptCount) return false
        if (version != other.version) return false
        if (identifier != other.identifier) return false
        if (!inputData.contentEquals(other.inputData)) return false
        if (!outputData.contentEquals(other.outputData)) return false
        if (!progressData.contentEquals(other.progressData)) return false
        if (tags != other.tags) return false

        return true
    }

    override fun hashCode(): Int {
        var result = runAttemptCount
        result = 31 * result + version
        result = 31 * result + identifier.hashCode()
        result = 31 * result + (inputData?.contentHashCode() ?: 0)
        result = 31 * result + (outputData?.contentHashCode() ?: 0)
        result = 31 * result + (progressData?.contentHashCode() ?: 0)
        result = 31 * result + tags.hashCode()
        return result
    }
}