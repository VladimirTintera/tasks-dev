package eu.tintera.tasks.core.migrations

import eu.tintera.tasks.TaskRegistration

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
                input = data.inputData?.let { bytes ->
                    migration.inputMigrator?.let {
                        val obj = acc.input ?: it.fromSerializer.decodeFromBytes(bytes)
                        it.migrationBlock(obj)
                    } ?: acc.input
                },
                output = data.outputData?.let { bytes ->
                    migration.outputMigrator?.let {
                        val obj = acc.output ?: it.fromSerializer.decodeFromBytes(bytes)
                        it.migrationBlock(obj)
                    } ?: acc.output
                },
                progress = data.progressData?.let { bytes ->
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