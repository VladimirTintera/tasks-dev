package eu.tintera.tasks

import eu.tintera.tasks.koin.startTasksKoin

object TasksInitializer {

    fun initialize(
        config: JvmTasksManagerConfiguration
    ) {
        startTasksKoin {
            modules(jvmModule(config))
        }
    }
}