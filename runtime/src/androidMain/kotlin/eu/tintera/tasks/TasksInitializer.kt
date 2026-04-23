package eu.tintera.tasks

import eu.tintera.tasks.runtime.TasksInitializerBase
import eu.tintera.tasks.runtime.androidModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.KoinApplication

object TasksInitializer : TasksInitializerBase<AndroidTasksConfiguration>() {

    override fun KoinApplication.customInitialization(config: AndroidTasksConfiguration) {
        androidContext(config.context)
    }

    override fun module(config: AndroidTasksConfiguration) = androidModule(config)
}