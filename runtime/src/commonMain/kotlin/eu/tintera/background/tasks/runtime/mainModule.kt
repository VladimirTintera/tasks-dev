package eu.tintera.background.tasks.runtime

import eu.tintera.background.tasks.Registry
import eu.tintera.background.tasks.core.RegistryResolver
import eu.tintera.background.tasks.db.databaseModule
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