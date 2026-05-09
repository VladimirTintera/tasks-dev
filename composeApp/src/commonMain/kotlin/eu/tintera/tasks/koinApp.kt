package eu.tintera.tasks

import co.touchlab.kermit.Logger
import eu.tintera.guard.ExecutionContextObserverRegistry
import eu.tintera.guard.ExecutionEnvironment
import eu.tintera.guard.ExhaustibleObservable
import eu.tintera.guard.MultiplexerObservable
import eu.tintera.guard.PendingTokenObservable
import eu.tintera.guard.TokenObservable
import eu.tintera.tasks.handlers.TestHandler
import eu.tintera.tasks.handlers.TestHandlerData
import eu.tintera.tasks.handlers.TestHandlerProgress
import eu.tintera.tasks.handlers.TestTypedTag
import eu.tintera.tasks.koin.json.taskHandlerOf
import eu.tintera.tasks.koin.taskTag
import eu.tintera.tasks.migrations.migration
import eu.tintera.tasks.migrations.migrations
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.createdAtStart
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.bind
import org.koin.dsl.binds
import org.koin.dsl.module

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

            taskTag<TestTypedTag>(serializer = TestTypedTag.serializer)
            taskHandlerOf(
                ::TestHandler,
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
            )
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