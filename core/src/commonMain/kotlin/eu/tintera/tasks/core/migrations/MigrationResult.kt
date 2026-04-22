package eu.tintera.tasks.core.migrations

data class MigrationResult(
    val input: Any? = null,
    val output: Any? = null,
    val progress: Any? = null,
    val version: Int
)