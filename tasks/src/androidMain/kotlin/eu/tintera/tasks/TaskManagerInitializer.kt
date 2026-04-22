package eu.tintera.tasks

import android.content.Context
import org.koin.android.ext.koin.androidContext

object TaskManagerInitializer {

    @Synchronized
    fun initialize(
        context: Context,
        config: AndroidTasksConfiguration = AndroidTasksConfiguration(),
        taskLifecycleObservers: List<TaskLifecycleObserver> = emptyList()
    ) {

        TaskManagerBootstrapper.initialize(
            taskLifecycleObservers = taskLifecycleObservers
        ) {
            androidContext(context)
            modules(androidModule(config = config))
        }
    }
}