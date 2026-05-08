package eu.tintera.tasks.runtime

import eu.tintera.tasks.TaskLifecycleObserver
import eu.tintera.tasks.TaskManager
import eu.tintera.tasks.TaskManagerConfiguration
import eu.tintera.tasks.Tasks
import org.koin.core.KoinApplication
import org.koin.core.module.Module

abstract class TasksInitializerBase {

    internal abstract fun module(config: TaskManagerConfiguration): Module

    internal open fun KoinApplication.customInitialization(config: TaskManagerConfiguration) {}

    internal fun create(
        config: TaskManagerConfiguration,
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