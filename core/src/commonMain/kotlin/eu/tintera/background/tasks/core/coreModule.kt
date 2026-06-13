package eu.tintera.background.tasks.core

import eu.tintera.background.tasks.core.cleanup.DatabaseCleaner
import eu.tintera.background.tasks.core.cleanup.DatabaseCleanupTaskHandler
import eu.tintera.background.tasks.core.migrations.TaskMigrator
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.dsl.createdAtStart
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val coreModule = module {
    singleOf<AppDispatchers>(::AppDispatchersImpl)
    single { ApplicationScope(SupervisorJob()) }
    factoryOf(::TaskResultHandlerImpl) bind TaskResultHandler::class
    factoryOf(::TaskEvaluatorImpl) bind TaskEvaluator::class
    singleOf(::DatabaseCleaner) {
        createdAtStart()
    }
    factoryOf(::DatabaseCleanupTaskHandler)
    factoryOf(::TaskMigrator)
    factoryOf(::TaskScopeFactory)
    factory {
        CompositeTaskLifecycleObserver(getAll())
    }
    factoryOf(::TagMapper)
}