package eu.tintera.tasks

import eu.tintera.tasks.runtime.TasksInitializerBase
import eu.tintera.tasks.runtime.jvmModule

actual object TasksInitializer : TasksInitializerBase(){
    actual fun initialize(
        configuration: TaskManagerConfiguration,
        taskLifecycleObservers: List<TaskLifecycleObserver>
    ) = create(configuration, taskLifecycleObservers)

    override fun module(config: TaskManagerConfiguration) = jvmModule(config)
}