package eu.tintera.tasks

import platform.Foundation.NSBundle

fun initialize() {
    val app = koinApp {

    }

    TasksInitializer.initialize(
        config = IosTasksManagerConfiguration(
            bgProcessingTaskIdentifier = (NSBundle.mainBundle.bundleIdentifier + ".BgProcessingTask"),
            appRefreshTaskIdentifier = (NSBundle.mainBundle.bundleIdentifier + ".AppRefreshTask")
        ),
        taskLifecycleObservers = app.koin.getAll()
    )
}