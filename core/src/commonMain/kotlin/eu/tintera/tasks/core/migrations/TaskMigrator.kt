package eu.tintera.tasks.core.migrations

import eu.tintera.tasks.core.TaskRegistry
import eu.tintera.tasks.core.data.Task

class TaskMigrator {

    fun migrate(
        task: Task,
        registration: TaskRegistry.TaskRegistration<Any, Any, Any>
    ): MigrationResult? {

        if (task.version == registration.currentVersion) return null

        val migrationsToRun = registration.migrations.findMigrationPath(
            startVersion = task.version,
            targetVersion = registration.currentVersion,
        )

        return migrationsToRun.fold(MigrationResult(version = task.version)) { acc, migration ->
            acc.copy(
                input = task.inputData?.let { bytes ->
                    migration.inputMigrator?.let {
                        val obj = acc.input ?: it.fromSerializer.decodeFromBytes(bytes)
                        it.migrationBlock(obj)
                    } ?: acc.input
                },
                output = task.outputData?.let { bytes ->
                    migration.outputMigrator?.let {
                        val obj = acc.output ?: it.fromSerializer.decodeFromBytes(bytes)
                        it.migrationBlock(obj)
                    } ?: acc.output
                },
                progress = task.progressData?.let { bytes ->
                    migration.progressMigrator?.let {
                        val obj = acc.progress ?: it.fromSerializer.decodeFromBytes(bytes)
                        it.migrationBlock(obj)
                    } ?: acc.progress
                },
                version = migration.endVersion
            )
        }
    }
}