package eu.tintera.tasks

import eu.tintera.tasks.runtime.TasksInitializerBase
import eu.tintera.tasks.runtime.androidModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.KoinApplication

actual object TasksInitializer  : TasksInitializerBase(){
    actual fun initialize(
        configuration: TaskManagerConfiguration,
        taskLifecycleObservers: List<TaskLifecycleObserver>
    ) = create(
        configuration, taskLifecycleObservers
    )

    override fun KoinApplication.customInitialization(config: TaskManagerConfiguration) {
        androidContext(config.context)
    }

    override fun module(config: TaskManagerConfiguration) = androidModule(config)
}