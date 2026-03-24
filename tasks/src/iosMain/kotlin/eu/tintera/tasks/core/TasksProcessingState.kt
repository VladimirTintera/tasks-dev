package eu.tintera.tasks.core

import kotlin.time.Instant

internal data class TasksProcessingState(
    val finished: Boolean,
    val nextProcessTime: Instant?
)