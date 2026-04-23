package eu.tintera.tasks.runtime

import eu.tintera.tasks.TaskLifecycleObserver
import eu.tintera.tasks.TaskManager
import eu.tintera.tasks.Tasks
import org.koin.core.KoinApplication
import org.koin.core.module.Module

abstract class TasksInitializerBase<T> {

    internal abstract fun module(config: T): Module

    open fun KoinApplication.customInitialization(config: T) {}
    fun initialize(
        config: T,
        taskLifecycleObservers: List<TaskLifecycleObserver> = emptyList()
    ): TaskManager {

        TaskManagerBootstrapper.initialize(
            taskLifecycleObservers = taskLifecycleObservers
        ) {
            customInitialization(config)
            modules(module(config))
        }

        return Tasks.taskManager
    }
}