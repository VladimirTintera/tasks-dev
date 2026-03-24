package eu.tintera.tasks.koin

import eu.tintera.tasks.TaskManager
import eu.tintera.tasks.core.TaskEvaluator
import eu.tintera.tasks.core.TaskManagerImpl
import eu.tintera.tasks.core.TaskRegistry
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

internal fun mainModule() = module {
    factoryOf(::TaskEvaluator)
    factoryOf(::TaskManagerImpl) bind TaskManager::class
    singleOf(::TaskRegistry)


}