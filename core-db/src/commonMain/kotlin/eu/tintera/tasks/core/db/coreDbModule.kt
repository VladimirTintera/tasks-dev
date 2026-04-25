package eu.tintera.tasks.core.db

import eu.tintera.tasks.core.data.TaskEvaluatorRepository
import eu.tintera.tasks.core.data.TaskScopeRepository
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module

val coreDbModule = module {
    factoryOf(::TaskEvaluatorRepositoryImpl) bind TaskEvaluatorRepository::class
    factoryOf(::TaskScopeRepositoryImpl) bind TaskScopeRepository::class
}