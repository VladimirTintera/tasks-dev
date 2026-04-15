package eu.tintera.tasks

import kotlin.time.Instant
import kotlin.uuid.Uuid

data class TaskInfo(
    val id: Uuid,
    val state: State,
    val tags: Set<String>,
    val runAttemptCount: Int,
    val outputData: Data,
    val nextScheduledTime: Instant?,
    val progress: Data,
    val finishedAt: Instant?,
    val createdAt: Instant
)