package eu.tintera.background.tasks

enum class TasksLogLevel { Debug, Info, Warning, Error }

/**
 * Šev pro diagnostiku knihovny. Bez registrovaného loggeru se nikam nic nepíše — knihovna nemá
 * názor na to, kam logy patří.
 *
 * Registruje se při inicializaci:
 * ```
 * TasksInitializer.initialize(
 *     configuration = …,
 *     loggers = listOf(TasksLogger { level, tag, message, throwable -> … })
 * )
 * ```
 *
 * Nezaměňovat s [TaskLifecycleObserver] — ten sleduje životní cyklus konkrétních tasků
 * (spuštěn / dokončen / zrušen). Sem chodí to ostatní: proč se task nepodařilo spustit, jak
 * dopadlo plánování background okna, spolknuté výjimky.
 */
fun interface TasksLogger {
    fun log(level: TasksLogLevel, tag: String, message: String, throwable: Throwable?)
}
