package eu.tintera.tasks

object TasksInitializer {
    fun initialize(
        config: IosTasksManagerConfiguration = IosTasksManagerConfiguration(),
        taskLifecycleObservers: List<TaskLifecycleObserver> = emptyList()
    ) {
        TaskManagerBootstrapper.initialize(
            taskLifecycleObservers = taskLifecycleObservers
        ) {
            modules(
                iosModule(config),
            )
        }
    }
}