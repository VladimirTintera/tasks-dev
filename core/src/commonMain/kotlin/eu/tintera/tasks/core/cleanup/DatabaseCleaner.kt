package eu.tintera.tasks.core.cleanup

import eu.tintera.tasks.*
import eu.tintera.tasks.migrations.migration
import eu.tintera.tasks.serialization.TaskDataSerializer
import kotlinx.coroutines.*
import kotlin.time.Duration.Companion.hours

internal class DatabaseCleaner(
    private val handler: DatabaseCleanupTaskHandler,
    private val taskManager: TaskManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private fun unitSerializer() = object : TaskDataSerializer<Unit> {
        override fun encodeToBytes(value: Unit): ByteArray {
            return byteArrayOf()
        }

        override fun decodeFromBytes(bytes: ByteArray) {

        }
    }

    init {
        taskManager.register(
            identifier = IDENTIFIER,
            currentVersion = 2,
            inputSerializer = unitSerializer(),
            outputSerializer = unitSerializer(),
            progressSerializer = unitSerializer(),
            factory = { handler },
            migrations = listOf(
                migration(startVersion = 1, endVersion = 2) {
                    migrateInput<Data, Unit>(unitSerializer(), unitSerializer()) { }
                    migrateOutput<Data, Unit> { }
                    migrateProgress<Data, Unit> { }
                }
            )
        )

        scope.launch {
            taskManager.enqueuePeriodicUniqueTask(
                task = taskRequest(
                    identifier = IDENTIFIER,
                    constraints = Constraints(requiresDeviceIdle = false),
                    data = Unit
                ),
                existingTaskPolicy = ExistingPeriodicTaskPolicy.Keep,
                repeatInterval = 24.hours,
            )
        }
    }

    companion object {
        private const val IDENTIFIER = "sys:task_manager_cleanup"
    }
}