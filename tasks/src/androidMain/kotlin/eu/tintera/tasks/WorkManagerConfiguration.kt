package eu.tintera.tasks

internal data class WorkManagerConfiguration(
    val compatTransformation: (Map<String, Any?>) -> ByteArray?
)