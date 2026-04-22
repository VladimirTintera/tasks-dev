package eu.tintera.tasks

object TasksInitializer : TasksInitializerBase<JvmTasksManagerConfiguration>() {
    override fun module(config: JvmTasksManagerConfiguration) = jvmModule(config)
}