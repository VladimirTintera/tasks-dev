package eu.tintera.tasks.koin

import eu.tintera.tasks.Registry
import eu.tintera.tasks.core.RegistryResolver
import eu.tintera.tasks.db.databaseModule
import eu.tintera.tasks.runtime.TaskRegistry
import org.koin.dsl.binds
import org.koin.dsl.module

internal fun mainModule() = module {
    includes(databaseModule)

    single {
        TaskRegistry
    } binds arrayOf(Registry::class, RegistryResolver::class)
}