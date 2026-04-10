package eu.tintera.tasks.core

import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.dsl.createdAtStart
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val coreModule = module {
    factoryOf(::TaskProcessorImpl) bind TaskProcessor::class
    singleOf(::TaskDispatcher) {
        createdAtStart()
    }
    single {
        ExecutionCapabilityEvaluator(
            providers = getAll(),
            appStateObserver = get()
        )
    }

    singleOf<AppDispatchers>(::RealDispatchers)
    single { ApplicationScope(SupervisorJob()) }
}