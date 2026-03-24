package eu.tintera.tasks.core

import eu.tintera.tasks.EventBus
import eu.tintera.tasks.TaskEvent
import eu.tintera.tasks.TaskResult
import eu.tintera.tasks.TaskScope

class TaskEvaluator(
    private val taskRegistry: TaskRegistry,
) {
    suspend fun TaskScope.handle(
        taskIdentifier: String,
    ): TaskResult? = taskRegistry.resolve(taskIdentifier)?.let {
        with(it) {
            run()
        }
    } ?: run {
        EventBus.send(TaskEvent.TaskFailed(taskIdentifier, "Task '$taskIdentifier' not registered", null))
        null
    }
}