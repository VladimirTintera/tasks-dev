package eu.tintera.tasks.koin

import eu.tintera.tasks.AndroidTasksConfiguration
import eu.tintera.tasks.TaskLifecycleObserver
import eu.tintera.tasks.TasksInitializer
import org.koin.core.module.Module

internal class TaskManagerBootstrapper(
    configuration: AndroidTasksConfiguration,
    observers: List<TaskLifecycleObserver>,
    onRegistered: () -> Unit
) {
    init {
        TasksInitializer.initialize(
            config = configuration,
            taskLifecycleObservers = observers
        )
        onRegistered()
    }
}

fun Module.taskManagerBootstrapper(
    onRegistered: () -> Unit = {}
) {
    single(createdAtStart = true) {
        TaskManagerBootstrapper(
            configuration = get(),
            observers = getAll(),
            onRegistered = onRegistered
        )
    }
}