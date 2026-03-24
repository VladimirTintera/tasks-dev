package eu.tintera.tasks.core

import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.dsl.createdAtStart
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val coreModule = module {
    factoryOf(::TaskProcessor)
    singleOf(::TaskDispatcher) {
        createdAtStart()
    }

    singleOf<AppDispatchers>(::RealDispatchers)
    single { ApplicationScope(SupervisorJob()) }
}