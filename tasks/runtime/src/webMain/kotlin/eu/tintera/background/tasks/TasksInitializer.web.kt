package eu.tintera.background.tasks

import eu.tintera.background.tasks.runtime.TasksInitializerBase

actual object TasksInitializer : TasksInitializerBase() {
    actual fun initialize(
        configuration: TaskManagerConfiguration,
        taskLifecycleObservers: List<TaskLifecycleObserver>
    ) = create(
        configuration, taskLifecycleObservers
    )

    override fun module(config: TaskManagerConfiguration) = webModule(config)
}