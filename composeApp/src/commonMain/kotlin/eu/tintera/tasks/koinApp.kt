package eu.tintera.tasks

import co.touchlab.kermit.Logger
import eu.tintera.koin.taskHandlerOf
import eu.tintera.koin.tasksKoinModule
import eu.tintera.tasks.handlers.TestHandler
import eu.tintera.tasks.legacy.legacySerializer
import eu.tintera.tasks.migrations.migration
import eu.tintera.tasks.serialization.jsonSerializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

fun koinApp(
    appDeclaration: KoinAppDeclaration
)  = startKoin {
    appDeclaration()
    modules(
        tasksKoinModule(),
        module {
            taskHandlerOf(::TestHandler,
                currentVersion = 2,
                migrations = listOf(
                    migration(1, 2) {
                        migrateInput(legacySerializer(), jsonSerializer()) {
                            it.getInt("count") ?: 20
                        }
                    }
                )
            )
            viewModelOf(::MainViewModel)

            single(createdAtStart = true) {
                object {
                    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
                    init {
                        scope.launch {
                            EventBus.events.collect {
                                when(it) {
                                    is TaskEvent.Custom -> Logger.i(tag = it.tag) { it.message }
                                }

                            }
                        }
                    }
                }


            }

            single(createdAtStart = true) {
                object {
                    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

                    init {
                        scope.launch {
                            eu.tintera.guard.EventBus.events.collect {
                                Logger.i(tag = it.tag) { it.message }
                            }
                        }
                    }
                }
            }
        }
    )
}