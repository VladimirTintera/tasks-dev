package eu.tintera.tasks

import eu.tintera.tasks.koin.Resolver
import org.koin.core.component.get

object Tasks {
    val taskManager: TaskManager get() = try {
        Resolver.get<TaskManager>()
    } catch (_: Throwable) {
        error("TaskManager is not initialized!")
    }

    val registry: Registry get() = TaskRegistry
}