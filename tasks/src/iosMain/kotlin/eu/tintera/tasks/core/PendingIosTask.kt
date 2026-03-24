package eu.tintera.tasks.core

import kotlin.time.Instant

data class PendingIosTask(
    val identifier: String,
    val earliestBeginTime: Instant?
)