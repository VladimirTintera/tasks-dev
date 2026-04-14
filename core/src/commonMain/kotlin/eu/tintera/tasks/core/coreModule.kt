package eu.tintera.tasks.core

import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module

val coreModule = module {
    factoryOf(::TaskResultProcessorImpl) bind TaskResultProcessor::class
}