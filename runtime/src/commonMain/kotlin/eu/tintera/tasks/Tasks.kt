package eu.tintera.tasks

import eu.tintera.tasks.runtime.Resolver
import eu.tintera.tasks.runtime.TaskRegistry
import org.koin.core.component.get

object Tasks {
    val taskManager: TaskManager
        get() = try {
            Resolver.get<TaskManager>()
        } catch (e: Throwable) {
            throw IllegalStateException("TaskManager is not initialized!", e)
        }

    val registry: Registry get() = TaskRegistry
}