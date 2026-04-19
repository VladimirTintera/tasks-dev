package eu.tintera.tasks

import androidx.work.Data
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class AndroidTasksConfiguration(
    val executionContextReleaseDebounce: Duration = 1.5.seconds,
    val compatTransformation: (Map<String, Any?>) -> ByteArray? = { null }
)