package eu.tintera.tasks.core.data

import eu.tintera.tasks.State
import eu.tintera.tasks.Tag
import eu.tintera.tasks.TaskInfo
import eu.tintera.tasks.TaskRegistration
import eu.tintera.tasks.core.migrations.MigratableData
import eu.tintera.tasks.core.migrations.MigrationResult
import kotlin.time.Instant
import kotlin.uuid.Uuid

data class Info(
    val id: Uuid,
    val identifier: String,
    val runAttemptCount: Int,
    val state: State,
    val tags: Set<String>,
    override val outputData: ByteArray?,
    val processTime: Instant?,
    override val progressData: ByteArray?,
    val finishedAt: Instant?,
    val createdAt: Instant,
    override val version: Int
) : MigratableData {

    override val inputData: ByteArray? = null
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Info

        if (runAttemptCount != other.runAttemptCount) return false
        if (version != other.version) return false
        if (id != other.id) return false
        if (identifier != other.identifier) return false
        if (state != other.state) return false
        if (tags != other.tags) return false
        if (!outputData.contentEquals(other.outputData)) return false
        if (processTime != other.processTime) return false
        if (!progressData.contentEquals(other.progressData)) return false
        if (finishedAt != other.finishedAt) return false
        if (createdAt != other.createdAt) return false
        if (!inputData.contentEquals(other.inputData)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = runAttemptCount
        result = 31 * result + version
        result = 31 * result + id.hashCode()
        result = 31 * result + identifier.hashCode()
        result = 31 * result + state.hashCode()
        result = 31 * result + tags.hashCode()
        result = 31 * result + (outputData?.contentHashCode() ?: 0)
        result = 31 * result + (processTime?.hashCode() ?: 0)
        result = 31 * result + (progressData?.contentHashCode() ?: 0)
        result = 31 * result + (finishedAt?.hashCode() ?: 0)
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + (inputData?.contentHashCode() ?: 0)
        return result
    }
}

fun <O : Any, P : Any> Info.toTaskInfo(
    registration: TaskRegistration<Any, O, P>?,
    migrationResult: MigrationResult?,
    tags: Set<Tag>
): TaskInfo {

    return TaskInfo(
        id = id,
        identifier = identifier,
        runAttemptCount = runAttemptCount,
        state = state,
        tags = tags,
        outputData = outputData?.let {
            migrationResult?.output ?: registration?.outputSerializer?.decodeFromBytes(it)
        },
        nextScheduledTime = processTime,
        progress = progressData?.let {
            migrationResult?.progress ?: registration?.progressSerializer?.decodeFromBytes(it)
        },
        finishedAt = finishedAt,
        createdAt = createdAt
    )
}