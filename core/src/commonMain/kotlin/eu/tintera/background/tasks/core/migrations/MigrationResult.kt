package eu.tintera.background.tasks.core.migrations

data class MigrationResult(
    val input: Any? = null,
    val output: Any? = null,
    val progress: Any? = null,
    val version: Int
)