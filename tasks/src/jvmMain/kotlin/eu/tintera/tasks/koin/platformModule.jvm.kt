package eu.tintera.tasks.koin

import eu.tintera.tasks.JvmExecutionContextProvider
import eu.tintera.tasks.JvmNetworkState
import eu.tintera.tasks.JvmTokenProvider
import eu.tintera.tasks.core.*
import eu.tintera.tasks.core.locks.ExecutionContextProvider
import eu.tintera.tasks.core.locks.TokenProvider
import eu.tintera.tasks.db.databaseModule
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

internal actual fun platformModule(): Module = module {

    includes(databaseModule, coreModule)

    factoryOf(::RepositoryTaskScopeFactory) bind TaskScopeFactory::class

    factoryOf(::RepositoryCoreTaskManager) bind CoreTaskManager::class


    singleOf<NetworkState>(::JvmNetworkState)


    singleOf(::JvmExecutionContextProvider) bind ExecutionContextProvider::class

    singleOf(::JvmTokenProvider) bind TokenProvider::class
}