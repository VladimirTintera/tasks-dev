package eu.tintera.tasks.android

data class WorkManagerConfiguration(
    val compatTransformation: (Map<String, Any?>) -> ByteArray?
)