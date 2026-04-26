package eu.tintera.tasks.engine.db

import eu.tintera.tasks.core.OrphanTaskSweeperRepository
import eu.tintera.tasks.core.TaskDispatcherRepository
import eu.tintera.tasks.core.TaskProcessorRepository
import eu.tintera.tasks.core.constraints.ParentsConstraintRepository
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module

val engineDbModule = module {
    factoryOf(::TaskProcessorRepositoryImpl) bind TaskProcessorRepository::class
    factoryOf(::TaskDispatcherRepositoryImpl) bind TaskDispatcherRepository::class
    factoryOf(::OrphanTaskRepositoryImpl) bind OrphanTaskSweeperRepository::class
    factoryOf(::ParentsConstraintRepositoryImpl) bind ParentsConstraintRepository::class
}