package eu.tintera.tasks.core.guard

import eu.tintera.guard.ExecutionContextObserverRegistry
import eu.tintera.guard.ExecutionContextProvider
import eu.tintera.guard.ExecutionEnvironment
import eu.tintera.guard.ExecutionEnvironmentConfig
import eu.tintera.guard.ExecutionEnvironmentFactory
import eu.tintera.guard.PlatformContext
import eu.tintera.guard.TokenProducerRegistry
import eu.tintera.tasks.core.ApplicationScope
import eu.tintera.tasks.core.ExecutionContextBootstrapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.plus
import org.koin.core.module.Module
import org.koin.dsl.binds

fun Module.guardInit(
    executionEnvironment: ExecutionEnvironment?,
    platformContext: PlatformContext,
    config: ExecutionEnvironmentConfig
) {
    single {
        config
    }

    single {
        executionEnvironment ?: ExecutionEnvironmentFactory.create(
            context = platformContext,
            scope = get<ApplicationScope>() + Dispatchers.Default,
            config = get(),
            additionalTokenProviders = getAll(),
            observers = getAll()
        )
    } binds arrayOf(
        ExecutionContextProvider::class,
        ExecutionContextObserverRegistry::class,
        TokenProducerRegistry::class
    )

    executionEnvironment?.also {
        single(createdAtStart = true) {
            ExecutionContextBootstrapper(
                observerRegistry = get(),
                tokenProducerRegistry = get(),
                observers = getAll(),
                tokenProducers = getAll()
            )
        }
    }
}