package eu.tintera.background.tasks

enum class TasksLogLevel { Debug, Info, Warning, Error }

/**
 * Diagnostics seam for the library. With no logger registered nothing is written anywhere — the
 * library has no opinion on where logs belong.
 *
 * Registered at initialization:
 * ```
 * TasksInitializer.initialize(
 *     configuration = …,
 *     loggers = listOf(TasksLogger { level, tag, message, throwable -> … })
 * )
 * ```
 *
 * Not to be confused with [TaskLifecycleObserver], which tracks the lifecycle of individual tasks
 * (started / completed / cancelled). Everything else lands here: why a task could not be started,
 * how scheduling a background window went, swallowed exceptions.
 */
fun interface TasksLogger {
    fun log(level: TasksLogLevel, tag: String, message: String, throwable: Throwable?)
}
