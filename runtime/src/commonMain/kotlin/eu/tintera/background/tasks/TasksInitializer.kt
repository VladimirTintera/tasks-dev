package eu.tintera.background.tasks

expect object TasksInitializer {

    fun initialize(
        configuration: TaskManagerConfiguration,
        taskLifecycleObservers: List<TaskLifecycleObserver> = emptyList()
    ) : TaskManager
}