package eu.tintera.tasks.cleanup

import eu.tintera.tasks.Constraints
import eu.tintera.tasks.ExistingPeriodicTaskPolicy
import eu.tintera.tasks.TaskManager
import eu.tintera.tasks.TaskRequest
import kotlinx.coroutines.*
import kotlin.time.Duration.Companion.hours

internal class DatabaseCleaner(
    private val handler: DatabaseCleanupTaskHandler,
    private val taskManager: TaskManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        taskManager.register(IDENTIFIER) { handler }

        scope.launch {
            taskManager.enqueuePeriodicUniqueTask(
                task = TaskRequest(
                    identifier = IDENTIFIER,
                    constraints = Constraints(requiresDeviceIdle = false)
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