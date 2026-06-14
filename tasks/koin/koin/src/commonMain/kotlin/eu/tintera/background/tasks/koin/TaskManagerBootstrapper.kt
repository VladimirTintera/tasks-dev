package eu.tintera.background.tasks.koin

import eu.tintera.background.tasks.TaskLifecycleObserver
import eu.tintera.background.tasks.TaskManagerConfiguration
import eu.tintera.background.tasks.TasksInitializer
import org.koin.core.module.Module

internal class TaskManagerBootstrapper(
    configuration: TaskManagerConfiguration,
    observers: List<TaskLifecycleObserver>,
    onRegistered: () -> Unit
) {
    init {
        TasksInitializer.initialize(
            configuration = configuration,
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