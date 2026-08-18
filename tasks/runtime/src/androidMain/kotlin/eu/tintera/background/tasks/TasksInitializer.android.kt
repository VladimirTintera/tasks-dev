package eu.tintera.background.tasks

import eu.tintera.background.tasks.runtime.TasksInitializerBase
import eu.tintera.background.tasks.runtime.androidModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.KoinApplication

actual object TasksInitializer  : TasksInitializerBase(){
    actual fun initialize(
        configuration: TaskManagerConfiguration,
        taskLifecycleObservers: List<TaskLifecycleObserver>,
        loggers: List<TasksLogger>
    ) = create(
        configuration, taskLifecycleObservers, loggers
    )

    override fun KoinApplication.customInitialization(config: TaskManagerConfiguration) {
        androidContext(config.context)
    }

    override fun module(config: TaskManagerConfiguration) = androidModule(config)
}