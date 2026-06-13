package eu.tintera.background.tasks.core.db

import eu.tintera.background.tasks.core.TaskResultProcessorRepository
import eu.tintera.background.tasks.core.data.TaskEvaluatorRepository
import eu.tintera.background.tasks.core.data.TaskScopeRepository
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module

val coreDbModule = module {
    factoryOf(::TaskEvaluatorRepositoryImpl) bind TaskEvaluatorRepository::class
    factoryOf(::TaskScopeRepositoryImpl) bind TaskScopeRepository::class
    factoryOf(::TaskResultProcessorRepositoryImpl) bind TaskResultProcessorRepository::class
}