package eu.tintera.tasks

import eu.tintera.tasks.koin.startTasksKoin

object TasksInitializer {
    fun initialize(
        config: IosTasksManagerConfiguration = IosTasksManagerConfiguration()
    ) {
        startTasksKoin {
            modules(
                iosModule(config),
            )
        }
    }
}