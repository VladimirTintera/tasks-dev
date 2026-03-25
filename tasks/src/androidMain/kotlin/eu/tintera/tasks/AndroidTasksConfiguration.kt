package eu.tintera.tasks

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class AndroidTasksConfiguration(
    val executionContextReleaseDebounce: Duration = 1.5.seconds
)