package eu.tintera.tasks

import eu.tintera.tasks.koin.taskManagerBootstrapper
import org.koin.dsl.module
import platform.Foundation.NSBundle

fun initialize() {

    koinApp {
        modules(
            module {
                single {
                    TaskManagerConfiguration(
                        bgProcessingTaskIdentifier = (NSBundle.mainBundle.bundleIdentifier + ".BgProcessingTask"),
                        appRefreshTaskIdentifier = (NSBundle.mainBundle.bundleIdentifier + ".AppRefreshTask")
                    )
                }

                taskManagerBootstrapper {
                    koin.loadModules(listOf(logModule), createEagerInstances = true)
                }
            }
        )
    }
}