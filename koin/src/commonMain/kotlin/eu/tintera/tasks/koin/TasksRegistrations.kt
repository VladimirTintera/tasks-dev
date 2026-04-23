package eu.tintera.tasks.koin

import eu.tintera.tasks.TaskManagerBootstrapper
import eu.tintera.tasks.TaskRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

internal class TasksRegistrations(
    registrations: List<TaskRegistration<*, *, *>>
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        scope.launch {
            val taskManager = TaskManagerBootstrapper.taskManager.filterNotNull().first()
            registrations.forEach {
                taskManager.register(it)
            }
        }
    }
}
