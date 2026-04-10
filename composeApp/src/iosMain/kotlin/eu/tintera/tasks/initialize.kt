package eu.tintera.tasks

import platform.Foundation.NSBundle

fun initialize() {
    TasksInitializer.initialize(
        config = IosTasksManagerConfiguration(
            bgProcessingTaskIdentifier = (NSBundle.mainBundle.bundleIdentifier + ".BgProcessingTask"),
            appRefreshTaskIdentifier = (NSBundle.mainBundle.bundleIdentifier + ".AppRefreshTask")
        )
    )
    koinApp {

    }
}