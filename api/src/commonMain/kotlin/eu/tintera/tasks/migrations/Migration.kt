package eu.tintera.tasks.migrations

class Migration internal constructor(
    val startVersion: Int,
    val endVersion: Int,
    val inputMigrator: FieldMigrator<Any?, Any?>?,
    val outputMigrator: FieldMigrator<Any?, Any?>?,
    val progressMigrator: FieldMigrator<Any?, Any?>?
)