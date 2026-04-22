package eu.tintera.tasks.db.entities

data class ExecutableTaskEntity(
    val identifier: String,
    val runAttemptCount: Int,
    val version: Int,
    val inputData: ByteArray?,
    val outputData: ByteArray?,
    val progressData: ByteArray?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ExecutableTaskEntity

        if (runAttemptCount != other.runAttemptCount) return false
        if (version != other.version) return false
        if (identifier != other.identifier) return false
        if (!inputData.contentEquals(other.inputData)) return false
        if (!outputData.contentEquals(other.outputData)) return false
        if (!progressData.contentEquals(other.progressData)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = runAttemptCount
        result = 31 * result + version
        result = 31 * result + identifier.hashCode()
        result = 31 * result + (inputData?.contentHashCode() ?: 0)
        result = 31 * result + (outputData?.contentHashCode() ?: 0)
        result = 31 * result + (progressData?.contentHashCode() ?: 0)
        return result
    }
}