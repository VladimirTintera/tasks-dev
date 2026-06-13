package eu.tintera.background.tasks.core.guard

import eu.tintera.background.guard.*
import eu.tintera.background.tasks.core.ApplicationScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.plus
import org.koin.core.module.Module
import org.koin.dsl.binds

fun Module.guardInit(
    executionEnvironment: ExecutionEnvironment?,
    config: ExecutionEnvironmentConfig
) {
    single {
        config
    }

    single {
        executionEnvironment ?: ExecutionEnvironmentFactory.create(
            context = get(),
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