package eu.tintera.tasks.core.locks

import kotlin.time.Duration

data class ExecutionContextConfig(
    val releaseDebounce: Duration
)