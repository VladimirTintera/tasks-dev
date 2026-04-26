package eu.tintera.tasks

import eu.tintera.tasks.runtime.TasksInitializerBase

object TasksInitializer : TasksInitializerBase<IosTasksManagerConfiguration>() {
    override fun module(config: IosTasksManagerConfiguration) = iosModule(config)
}