package eu.tintera.tasks

import eu.tintera.koin.taskHandlerOf
import eu.tintera.koin.tasksKoinModule
import eu.tintera.tasks.handlers.TestHandler
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

fun koinApp(
    appDeclaration: KoinAppDeclaration
)  = startKoin {
    appDeclaration()
    modules(
        tasksKoinModule(),
        module {
            taskHandlerOf(::TestHandler)
        }
    )
}