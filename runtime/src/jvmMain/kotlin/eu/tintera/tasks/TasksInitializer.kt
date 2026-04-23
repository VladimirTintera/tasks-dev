package eu.tintera.tasks

import eu.tintera.tasks.runtime.TasksInitializerBase
import eu.tintera.tasks.runtime.jvmModule

object TasksInitializer : TasksInitializerBase<JvmTasksManagerConfiguration>() {
    override fun module(config: JvmTasksManagerConfiguration) = jvmModule(config)
}