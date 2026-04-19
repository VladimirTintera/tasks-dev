package eu.tintera.tasks.core

import eu.tintera.tasks.core.data.Repository
import eu.tintera.tasks.core.data.Task
import eu.tintera.tasks.core.migrations.findMigrationPath
import eu.tintera.tasks.migrations.FieldMigrator

internal class TaskMigrator(
    private val repository: Repository
) {

    suspend fun migrate(
        task: Task,
        registration: TaskRegistry.TaskRegistration<Any, Any, Any>
    ): Any? {

        val migrationsToRun = registration.migrations.findMigrationPath(
            startVersion = task.version,
            targetVersion = registration.currentVersion,
        )

        val taskData = migrationsToRun.fold(
            TaskData(
                inputBytes = task.inputData,
                outputBytes = task.outputData,
                progressBytes = task.progressData,
                parsedInput = null,
                version = task.version
            )
        ) { data, migration ->

            val nextInput = data.inputBytes?.let { bytes ->
                migration.inputMigrator?.apply(bytes) ?: (bytes to data.parsedInput)
            }

            TaskData(
                inputBytes = nextInput?.first,
                parsedInput = nextInput?.second,

                outputBytes = data.outputBytes?.let {
                    migration.outputMigrator?.apply(it)?.first ?: it
                },
                progressBytes = data.progressBytes?.let {
                    migration.progressMigrator?.apply(it)?.first ?: it
                },
                version = migration.endVersion
            )
        }

        if (migrationsToRun.isNotEmpty()) repository.upgradeData(
            id = task.id,
            input = taskData.inputBytes,
            output = taskData.outputBytes,
            progress = taskData.progressBytes,
            version = taskData.version
        )

        return taskData.parsedInput ?: taskData.inputBytes?.let {
            registration.inputSerializer.decodeFromBytes(it)
        }
    }

    private fun <From, To> FieldMigrator<From, To>.apply(bytes: ByteArray): Pair<ByteArray, To> {
        val oldObj = fromSerializer.decodeFromBytes(bytes)
        val newObj = migrationBlock(oldObj)
        val newBytes = toSerializer.encodeToBytes(newObj)

        return Pair(newBytes, newObj)
    }

    private class TaskData(
        val inputBytes: ByteArray?,
        val outputBytes: ByteArray?,
        val progressBytes: ByteArray?,
        val parsedInput: Any? = null,
        val version: Int
    )
}