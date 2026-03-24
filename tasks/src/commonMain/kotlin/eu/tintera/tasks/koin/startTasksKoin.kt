package eu.tintera.tasks.koin

import org.koin.core.KoinApplication
import org.koin.dsl.koinApplication

internal fun startTasksKoin(koinAppInitialization: KoinApplication.() -> Unit = {}): KoinApplication {
    TasksKoinContext.koinApp = koinApplication {
        koinAppInitialization()
        modules(
            mainModule(),
            platformModule()
        )
    }

    return TasksKoinContext.koinApp
}