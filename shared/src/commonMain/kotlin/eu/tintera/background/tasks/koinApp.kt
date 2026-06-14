package eu.tintera.background.tasks

import co.touchlab.kermit.Logger
import eu.tintera.background.guard.*
import eu.tintera.background.tasks.handlers.TestHandler
import eu.tintera.background.tasks.handlers.TestHandlerData
import eu.tintera.background.tasks.handlers.TestHandlerProgress
import eu.tintera.background.tasks.handlers.TestTypedTag
import eu.tintera.background.tasks.koin.json.taskHandler
import eu.tintera.background.tasks.koin.taskTag
import eu.tintera.background.tasks.migrations.migration
import eu.tintera.background.tasks.migrations.migrations
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.bind
import org.koin.dsl.binds
import org.koin.dsl.module
import org.koin.plugin.module.dsl.factory
import org.koin.plugin.module.dsl.viewModel

fun koinApp(
    appDeclaration: KoinAppDeclaration = {}
) = startKoin {
    appDeclaration()
    modules(
        module {

            single {
                Tasks.taskManager
            }

            single {
                ApplicationScope(SupervisorJob())
            }

            single {
                Tasks.executionEnvironment
            } binds arrayOf(
                ExecutionEnvironment::class,
                TokenObservable::class,
                ExhaustibleObservable::class,
                PendingTokenObservable::class,
                ExecutionContextObserverRegistry::class,
                MultiplexerObservable::class
            )

            factoryOf(::TasksObserver) bind TaskLifecycleObserver::class

            taskTag<TestTypedTag>(
                identifier = "eu.tintera.tasks.handlers.TestTypedTag",
                serializer = TestTypedTag.serializer
            )

            taskHandler(
                identifier = "eu.tintera.tasks.handlers.TestHandler",
                currentVersion = 2,
                migrations = migrations {
                    migration(1, 2) {
                        input(legacySerializer()) {
                            TestHandlerData(
                                count = it.getInt("count") ?: 20
                            )
                        }
                        progress(legacySerializer()) {
                            TestHandlerProgress(
                                totalCount = it.getInt("totalCount") ?: 20,
                                progress = it.getInt("progress") ?: 0
                            )
                        }
                        output(legacySerializer()) {
                            TestHandlerData(
                                count = it.getInt("count") ?: 20
                            )
                        }
                    }
                }
            ) {
                factoryOf(::TestHandler)
            }

            viewModelOf(::MainViewModel)

            single(createdAtStart = true) {
                object {
                    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

                    init {
                        scope.launch {
                            EventBus.events.collect {
                                when (it) {
                                    is TaskEvent.Custom -> Logger.i(tag = it.tag) { it.message }
                                }

                            }
                        }
                    }
                }
            }
        }
    )
}