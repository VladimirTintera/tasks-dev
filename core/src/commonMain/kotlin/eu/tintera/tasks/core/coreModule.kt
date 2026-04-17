package eu.tintera.tasks.core

import eu.tintera.tasks.core.cleanup.DatabaseCleaner
import eu.tintera.tasks.core.cleanup.DatabaseCleanupTaskHandler
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.dsl.createdAtStart
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val coreModule = module {
    singleOf<AppDispatchers>(::RealDispatchers)
    single { ApplicationScope(SupervisorJob()) }
    factoryOf(::TaskResultProcessorImpl) bind TaskResultProcessor::class
    factoryOf(::TaskEvaluatorImpl) bind TaskEvaluator::class
    singleOf(::TaskRegistry)
    singleOf(::DatabaseCleaner) {
        createdAtStart()
    }
    factoryOf(::DatabaseCleanupTaskHandler)
}