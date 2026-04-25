package eu.tintera.tasks.runtime

import eu.tintera.tasks.Registry
import eu.tintera.tasks.core.RegistryResolver
import eu.tintera.tasks.db.databaseModule
import org.koin.dsl.binds
import org.koin.dsl.module
import kotlin.time.Clock

internal fun mainModule() = module {

    single<Clock> { Clock.System }

    includes(databaseModule)

    single {
        taskRegistry
    } binds arrayOf(Registry::class, RegistryResolver::class)
}