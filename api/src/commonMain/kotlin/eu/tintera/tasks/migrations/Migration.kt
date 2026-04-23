package eu.tintera.tasks.migrations

class Migration internal constructor(
    val startVersion: Int,
    val endVersion: Int,
    val inputMigrator: Migrator<Any, Any>?,
    val outputMigrator: Migrator<Any, Any>?,
    val progressMigrator: Migrator<Any, Any>?
)