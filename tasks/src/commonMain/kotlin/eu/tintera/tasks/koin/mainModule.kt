package eu.tintera.tasks.koin

import eu.tintera.tasks.TaskManager
import eu.tintera.tasks.core.TaskManagerImpl
import eu.tintera.tasks.db.databaseModule
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module

internal fun mainModule() = module {
    includes(databaseModule)

    factoryOf(::TaskManagerImpl) bind TaskManager::class
}