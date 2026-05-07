package eu.tintera.tasks

import eu.tintera.guard.ExecutionEnvironment
import eu.tintera.tasks.runtime.Resolver
import eu.tintera.tasks.runtime.taskRegistry
import org.koin.core.component.get

object Tasks {
    val taskManager: TaskManager
        get() = try {
            Resolver.get<TaskManager>()
        } catch (e: Throwable) {
            throw IllegalStateException("TaskManager is not yet initialized!", e)
        }

    val registry: Registry get() = taskRegistry

    val executionEnvironment: ExecutionEnvironment
        get() = try {
            Resolver.get<ExecutionEnvironment>()
        } catch (e: Throwable) {
            throw IllegalStateException("TaskManager is not yet initialized!")
        }
}