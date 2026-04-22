package eu.tintera.tasks

import org.koin.core.KoinApplication
import org.koin.core.module.Module

abstract class TasksInitializerBase<T> {

    internal abstract fun module(config: T): Module

    open fun KoinApplication.customInitialization(config: T) {}
    fun initialize(
        config: T,
        taskLifecycleObservers: List<TaskLifecycleObserver> = emptyList()
    ) {
        TaskManagerBootstrapper.initialize(
            taskLifecycleObservers = taskLifecycleObservers
        ) {
            customInitialization(config)
            modules(module(config))
        }
    }
}