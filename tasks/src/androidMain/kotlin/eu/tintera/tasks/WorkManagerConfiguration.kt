package eu.tintera.tasks

import androidx.work.Data

internal data class WorkManagerConfiguration(
    val compatTransformation: (Map<String, Any?>) -> ByteArray?
)