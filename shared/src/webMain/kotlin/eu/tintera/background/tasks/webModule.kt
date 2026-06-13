package eu.tintera.background.tasks

import eu.tintera.background.tasks.koin.taskManagerBootstrapper
import org.koin.core.KoinApplication
import org.koin.dsl.module


val KoinApplication.webModule
    get() = module {

        single {
            TaskManagerConfiguration()
        }

        taskManagerBootstrapper {
            println("Bootstrapped")
            koin.loadModules(listOf(logModule), createEagerInstances = true)
        }
    }