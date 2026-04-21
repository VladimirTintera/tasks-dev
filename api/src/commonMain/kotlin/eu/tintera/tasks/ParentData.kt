package eu.tintera.tasks

import kotlin.time.Instant
import kotlin.uuid.Uuid

data class ParentData(
    val id: Uuid,
    val identifier: String,
    val data: Any?,
    val finishedAt: Instant
)