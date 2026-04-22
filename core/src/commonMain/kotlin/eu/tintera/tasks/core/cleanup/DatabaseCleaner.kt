package eu.tintera.tasks.core.cleanup

import eu.tintera.tasks.Constraints
import eu.tintera.tasks.ExistingPeriodicTaskPolicy
import eu.tintera.tasks.TaskManager
import eu.tintera.tasks.core.AppDispatchers
import eu.tintera.tasks.core.ApplicationScope
import eu.tintera.tasks.core.serialization.UnitTaskDataSerializer
import eu.tintera.tasks.serialization.TaskDataSerializer
import eu.tintera.tasks.taskRequest
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.hours

internal class DatabaseCleaner(
    private val handler: DatabaseCleanupTaskHandler,
    private val taskManager: TaskManager,
    scope: ApplicationScope,
    dispatchers: AppDispatchers
) {
    init {
        taskManager.register(
            identifier = IDENTIFIER,
            inputSerializer = UnitTaskDataSerializer,
            outputSerializer = UnitTaskDataSerializer,
            progressSerializer = UnitTaskDataSerializer,
            factory = { handler },
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
        private const val IDENTIFIER = "sys:task_manager_cleanup"
    }
}