package eu.tintera.background.tasks

import eu.tintera.background.guard.ExecutionEnvironment
import eu.tintera.background.tasks.runtime.Resolver
import eu.tintera.background.tasks.runtime.taskRegistry
import org.koin.core.component.get

/** Entry point to the running library. Everything here requires [TasksInitializer.initialize] first. */
object Tasks {

    val taskManager: TaskManager get() = resolveOrExplain { Resolver.get<TaskManager>() }

    val registry: Registry get() = taskRegistry

    val executionEnvironment: ExecutionEnvironment get() = resolveOrExplain { Resolver.get<ExecutionEnvironment>() }

    /**
     * Turns the internal DI failure into the answer the caller actually needs.
     *
     * Resolving before initialization fails deep inside Koin with a message about a missing
     * definition, which points at the library's internals rather than at the real mistake. The
     * original is kept as the cause, so nothing is lost.
     */
    private inline fun <T> resolveOrExplain(resolve: () -> T): T = try {
        resolve()
    } catch (e: Throwable) {
        throw IllegalStateException(
            "TaskManager is not initialized yet. Call TasksInitializer.initialize(...) first.",
            e,
        )
    }
}
