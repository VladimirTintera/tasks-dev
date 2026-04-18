package eu.tintera.tasks

import kotlin.time.Instant

data class ParentData(
    val id: String,
    val identifier: String,
    val data: Any?,
    val finishedAt: Instant
)