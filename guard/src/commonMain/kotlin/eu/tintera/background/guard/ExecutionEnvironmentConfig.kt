package eu.tintera.background.guard

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class ExecutionEnvironmentConfig(
    val releaseDebounce: Duration = 1.5.seconds
)