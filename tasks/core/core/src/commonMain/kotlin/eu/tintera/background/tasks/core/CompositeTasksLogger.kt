package eu.tintera.background.tasks.core

import eu.tintera.background.tasks.TasksLogLevel
import eu.tintera.background.tasks.TasksLogger

/**
 * Rozešle záznam všem registrovaným loggerům. Injektuje se **konkrétním typem**, ne přes
 * [TasksLogger] — jinak by `getAll()` uvnitř vrátilo i tuhle instanci a zacyklilo se.
 * Stejný důvod jako u [CompositeTaskLifecycleObserver].
 */
class CompositeTasksLogger(
    private val loggers: List<TasksLogger>
) : TasksLogger {

    override fun log(level: TasksLogLevel, tag: String, message: String, throwable: Throwable?) {
        // Vadný logger nesmí shodit task, který se jen snažil něco zalogovat.
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
