package eu.tintera.background.tasks

import eu.tintera.background.tasks.runtime.TasksInitializerBase
import eu.tintera.background.tasks.runtime.jvmModule

actual object TasksInitializer : TasksInitializerBase(){
    actual fun initialize(
        configuration: TaskManagerConfiguration,
        taskLifecycleObservers: List<TaskLifecycleObserver>,
        loggers: List<TasksLogger>
    ) = create(configuration, taskLifecycleObservers, loggers)

    override fun module(config: TaskManagerConfiguration) = jvmModule(config)
}