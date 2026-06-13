package eu.tintera.background.tasks

import eu.tintera.background.guard.ExecutionEnvironment
import eu.tintera.background.tasks.runtime.Resolver
import eu.tintera.background.tasks.runtime.taskRegistry
import org.koin.core.component.get

object Tasks {
    val taskManager: TaskManager
        get() = try {
            Resolver.get<TaskManager>()
        } catch (e: Throwable) {
            throw e
            //throw IllegalStateException("TaskManager is not yet initialized!", e)
        }

    val registry: Registry get() = taskRegistry

    val executionEnvironment: ExecutionEnvironment
        get() = try {
            Resolver.get<ExecutionEnvironment>()
        } catch (e: Throwable) {
            throw e
            //throw IllegalStateException("TaskManager is not yet initialized!", e)
        }
}