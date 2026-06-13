package eu.tintera.background.tasks

import eu.tintera.background.tasks.koin.taskManagerBootstrapper
import org.koin.core.KoinApplication
import org.koin.dsl.module

val KoinApplication.androidModule get() = module {
    single {
        TaskManagerConfiguration(
            context = get(),
            compatTransformation = {
                it.toByteArray()
            }
        )
    }

    taskManagerBootstrapper {
        koin.loadModules(listOf(logModule), createEagerInstances = true)
    }
}