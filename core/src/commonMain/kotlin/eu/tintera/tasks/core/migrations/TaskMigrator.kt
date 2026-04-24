package eu.tintera.tasks.core.migrations

import eu.tintera.tasks.TaskRegistration
import eu.tintera.tasks.migrations.Migrator

class TaskMigrator {

    fun <Input : Any, Output : Any, Progress : Any> migrate(
        data: MigratableData,
        registration: TaskRegistration<Input, Output, Progress>
    ): MigrationResult? {

        if (data.version == registration.currentVersion) return null

        val migrationsToRun = registration.migrations.findMigrationPath(
            startVersion = data.version,
            targetVersion = registration.currentVersion,
        )

        return migrationsToRun.fold(MigrationResult(version = data.version)) { acc, migration ->
            acc.copy(
                input = data.inputData.migrate(acc.input, migration.inputMigrator),
                output = data.outputData.migrate(acc.output, migration.outputMigrator),
                progress = data.progressData.migrate(acc.progress, migration.progressMigrator),
                version = migration.endVersion
            )
        }
    }

    private fun ByteArray?.migrate(
        prevObj: Any?,
        migrator: Migrator<Any, Any>?
    ): Any? = this?.let { bytes ->
        migrator?.let {
            val obj = prevObj ?: migrator.fromSerializer.decodeFromBytes(bytes)
            migrator.migrationBlock(obj)
        }
    } ?: prevObj
}