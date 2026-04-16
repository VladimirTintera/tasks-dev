package eu.tintera.tasks.core.cleanup

import eu.tintera.tasks.*
import kotlinx.coroutines.*
import kotlin.time.Duration.Companion.hours

internal class DatabaseCleaner(
    private val handler: DatabaseCleanupTaskHandler,
    private val taskManager: TaskManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        taskManager.register(
            identifier = IDENTIFIER,
            currentVersion = 1
        ) { handler }

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