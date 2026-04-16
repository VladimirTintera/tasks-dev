package eu.tintera.tasks.core

import eu.tintera.tasks.core.cleanup.DatabaseCleaner
import eu.tintera.tasks.core.cleanup.DatabaseCleanupTaskHandler
import eu.tintera.tasks.core.seriaization.SerializationEngine
import org.koin.core.module.dsl.createdAtStart
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val coreModule = module {
    factoryOf(::TaskResultProcessorImpl) bind TaskResultProcessor::class
    factoryOf(::TaskEvaluatorImpl) bind TaskEvaluator::class
    single {
        SerializationEngine()
    }
    singleOf(::TaskRegistry)
    singleOf(::DatabaseCleaner) {
        createdAtStart()
    }

    factoryOf(::DatabaseCleanupTaskHandler)
}