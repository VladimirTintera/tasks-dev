package eu.tintera.tasks

import eu.tintera.tasks.runtime.TasksInitializerBase
import eu.tintera.tasks.runtime.iosModule

object TasksInitializer : TasksInitializerBase<IosTasksManagerConfiguration>() {
    override fun module(config: IosTasksManagerConfiguration) = iosModule(config)
}