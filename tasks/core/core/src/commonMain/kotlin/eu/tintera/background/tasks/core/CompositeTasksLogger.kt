package eu.tintera.background.tasks.core

import eu.tintera.background.tasks.TasksLogLevel
import eu.tintera.background.tasks.TasksLogger

/**
 * Fans a record out to every registered logger. Injected by its **concrete type**, not through
 * [TasksLogger] — otherwise `getAll()` inside would return this instance as well and recurse.
 * Same reason as [CompositeTaskLifecycleObserver].
 */
class CompositeTasksLogger(
    private val loggers: List<TasksLogger>
) : TasksLogger {

    override fun log(level: TasksLogLevel, tag: String, message: String, throwable: Throwable?) {
        // A faulty logger must not bring down a task that was only trying to log something.
        loggers.forEach { runCatching { it.log(level, tag, message, throwable) } }
    }

    fun debug(tag: String, throwable: Throwable? = null, message: () -> String) =
        log(TasksLogLevel.Debug, tag, message(), throwable)

    fun info(tag: String, throwable: Throwable? = null, message: () -> String) =
        log(TasksLogLevel.Info, tag, message(), throwable)

    fun warning(tag: String, throwable: Throwable? = null, message: () -> String) =
        log(TasksLogLevel.Warning, tag, message(), throwable)

    fun error(tag: String, throwable: Throwable? = null, message: () -> String) =
        log(TasksLogLevel.Error, tag, message(), throwable)
}
