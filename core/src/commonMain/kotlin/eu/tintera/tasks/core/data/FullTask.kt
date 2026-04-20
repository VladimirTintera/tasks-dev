package eu.tintera.tasks.core.data

import eu.tintera.tasks.TaskInfo
import eu.tintera.tasks.core.TaskRegistry
import eu.tintera.tasks.core.migrations.MigrationResult

data class FullTask(
    val task: Task,
    val tags: Set<String>
)

fun <O : Any, P : Any> FullTask.toTaskInfo(
    registration: TaskRegistry.TaskRegistration<Any, O, P>?,
    migrationResult: MigrationResult?
): TaskInfo {

    return TaskInfo(
        id = task.id,
        identifier = task.identifier,
        runAttemptCount = task.runAttemptCount,
        state = task.state,
        tags = tags,
        outputData = task.outputData?.let {
            migrationResult?.output ?: registration?.outputSerializer?.decodeFromBytes(it)
        },
        nextScheduledTime = task.processTime,
        progress = task.progressData?.let {
            migrationResult?.progress ?: registration?.progressSerializer?.decodeFromBytes(it)
        },
        finishedAt = task.finishedAt,
        createdAt = task.createdAt
    )
}