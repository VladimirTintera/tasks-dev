package eu.tintera.tasks

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

data class TaskManagerConfig(
    val maxConcurrentTasks: Int = 10,
    val executionContextReleaseDebounce: Duration = 1500.milliseconds
)