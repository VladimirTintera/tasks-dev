package eu.tintera.tasks

import co.touchlab.kermit.Logger
import eu.tintera.koin.taskHandlerOf
import eu.tintera.koin.tasksKoinModule
import eu.tintera.tasks.handlers.TestHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
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

            single(createdAtStart = true) {
                val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
                scope.launch {
                    EventBus.events.collect {
                        when(it) {
                            is TaskEvent.Custom -> Logger.i(it.tag) { it.message }
                            else -> Logger.i { it.toString() }
                        }

                    }
                }
            }

            single(createdAtStart = true) {
                val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
                scope.launch {
                    eu.tintera.guard.EventBus.events.collect {
                        Logger.i(it.tag) { it.message }
                    }
                }
            }
        }
    )
}