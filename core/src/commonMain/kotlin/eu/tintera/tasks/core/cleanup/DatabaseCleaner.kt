package eu.tintera.tasks.core.cleanup

import eu.tintera.tasks.*
import eu.tintera.tasks.core.AppDispatchers
import eu.tintera.tasks.core.ApplicationScope
import eu.tintera.tasks.core.io
import eu.tintera.tasks.core.serialization.UnitSerializer
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.hours

internal class DatabaseCleaner(
    private val handler: DatabaseCleanupTaskHandler,
    registry: Registry,
    private val taskManager: TaskManager,
    scope: ApplicationScope,
    dispatchers: AppDispatchers
) {
    init {
        registry.register(
            TaskRegistration(
                identifier = IDENTIFIER,
                inputSerializer = UnitSerializer,
                outputSerializer = UnitSerializer,
                progressSerializer = UnitSerializer,
                factory = { handler },
                currentVersion = 1,
                migrations = emptyList(),
                type = DatabaseCleanupTaskHandler::class
            )
        )

        scope.launch(dispatchers.io) {
            taskManager.enqueuePeriodicUniqueTask(
                task = taskRequest(
                    identifier = IDENTIFIER,
                    constraints = Constraints(requiresDeviceIdle = false),
                    data = Unit
                ),
                existingTaskPolicy = ExistingPeriodicTaskPolicy.Replace,
                repeatInterval = 24.hours,
            )
        }
    }

    companion object {
        const val IDENTIFIER = "sys:task_manager_cleanup"
    }
}