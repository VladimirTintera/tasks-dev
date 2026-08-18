package eu.tintera.background.tasks

import kotlin.time.Instant
import kotlin.uuid.Uuid

data class TaskInfo(
    val id: Uuid,
    val identifier: String,
    val state: State,
    val tags: Set<Tag>,
    val runAttemptCount: Int,
    val outputData: Any?,
    val nextScheduledTime: Instant?,
    val progress: Any?,
    val finishedAt: Instant?,
    val createdAt: Instant
)

inline fun <reified T> TaskInfo.outputAs(): T? {
    // Explicit type check so a wrong type argument produces a descriptive error instead of a bare
    // ClassCastException.
    if (outputData != null && outputData !is T) {
        error("Task '$id': cannot cast outputData of type ${outputData::class.simpleName} to ${T::class.simpleName}")
    }
    return outputData
}

inline fun <reified T> TaskInfo.progressAs(): T? {
    if (progress != null && progress !is T) {
        error("Task '$id': cannot cast progress of type ${progress::class.simpleName} to ${T::class.simpleName}")
    }
    return progress
}