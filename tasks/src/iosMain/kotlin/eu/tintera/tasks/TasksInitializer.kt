package eu.tintera.tasks

object TasksInitializer : TasksInitializerBase<IosTasksManagerConfiguration>() {
    override fun module(config: IosTasksManagerConfiguration) = iosModule(config)
}