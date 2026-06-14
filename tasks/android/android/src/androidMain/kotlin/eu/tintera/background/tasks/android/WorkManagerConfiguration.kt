package eu.tintera.background.tasks.android

data class WorkManagerConfiguration(
    val compatTransformation: (Map<String, Any?>) -> ByteArray?
)