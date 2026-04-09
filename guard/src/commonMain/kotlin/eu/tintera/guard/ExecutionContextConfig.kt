package eu.tintera.guard

import kotlin.time.Duration

data class ExecutionContextConfig(
    val releaseDebounce: Duration
)