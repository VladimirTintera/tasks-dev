package eu.tintera.tasks.core.data

import eu.tintera.tasks.TaskInfo
import eu.tintera.tasks.core.TaskRegistry

data class FullTask(
    val task: Task,
    val tags: Set<String>
)

fun <O: Any, P: Any> FullTask.toTaskInfo(
    registration: TaskRegistry.TaskRegistration<Any, O, P>?
): TaskInfo {
    return TaskInfo(
        id = task.id,
        identifier = task.identifier,
        runAttemptCount = task.runAttemptCount,
        state = task.state,
        tags = tags,
        outputData = task.outputData?.let {
            registration?.outputSerializer?.decodeFromBytes(it)
        },
        nextScheduledTime = task.processTime,
        progress = task.progressData?.let {
            registration?.progressSerializer?.decodeFromBytes(it)
        },
        finishedAt = task.finishedAt,
        createdAt = task.createdAt
    )
}