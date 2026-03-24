package eu.tintera.tasks.koin

import eu.tintera.tasks.IosNetworkState
import eu.tintera.tasks.core.*
import eu.tintera.tasks.core.locks.ExecutionContextProvider
import eu.tintera.tasks.db.databaseModule
import org.koin.core.module.Module
import org.koin.core.module.dsl.createdAtStart
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import platform.Foundation.NSBundle

internal actual fun platformModule(): Module = module {

    includes(databaseModule, coreModule)

    factoryOf(::IosTaskScopeFactory) bind TaskScopeFactory::class

    factoryOf(::RepositoryCoreTaskManager) bind CoreTaskManager::class


    singleOf<NetworkState>(::IosNetworkState)

    single(createdAtStart = true) {
        BgTaskManager(
            appPackage = NSBundle.mainBundle.bundleIdentifier ?: "eu.tintera.tasks",
            repository = get()
        )
    }

    singleOf(::IosExecutionContextProvider) bind ExecutionContextProvider::class

    singleOf(::AppLifecycleManager) {
        createdAtStart()
    }
    singleOf(::IosTokenProvider)
}