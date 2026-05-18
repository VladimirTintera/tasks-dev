package eu.tintera.tasks

import eu.tintera.tasks.koin.taskManagerBootstrapper
import org.koin.core.KoinApplication
import org.koin.dsl.module

val KoinApplication.jvmModule get() = module {
    single {
        TaskManagerConfiguration("TasksApp")
    }

    taskManagerBootstrapper {
        koin.loadModules(listOf(logModule), createEagerInstances = true)
    }
}