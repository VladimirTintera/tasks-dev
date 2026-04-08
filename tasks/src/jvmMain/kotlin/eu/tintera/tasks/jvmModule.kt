package eu.tintera.tasks

import androidx.sqlite.SQLiteDriver
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import eu.tintera.tasks.core.*
import eu.tintera.tasks.core.locks.ExecutionContextConfig
import eu.tintera.tasks.core.locks.ExecutionContextProvider
import eu.tintera.tasks.core.locks.TokenProvider
import eu.tintera.tasks.db.DatabaseConfiguration
import eu.tintera.tasks.db.JvmDatabaseConfiguration
import eu.tintera.tasks.db.databaseModule
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.binds
import org.koin.dsl.module

internal fun jvmModule(
    config: JvmTasksManagerConfiguration
): Module = module {

    includes(databaseModule, coreModule)

    factoryOf(::RepositoryTaskScopeFactory) bind TaskScopeFactory::class

    factoryOf(::RepositoryCoreTaskManager) bind CoreTaskManager::class

    singleOf<NetworkState>(::JvmNetworkState)

    singleOf(::JvmExecutionContextProvider) bind ExecutionContextProvider::class

    singleOf(::JvmTokenProvider) bind TokenProvider::class

    single {
        TaskProcessorConfig(
            maxConcurrentTasks = config.maxConcurrentTasks
        )
    }
    single {
        ExecutionContextConfig(
            releaseDebounce = config.executionContextReleaseDebounce
        )
    }

    single<SQLiteDriver> { BundledSQLiteDriver() }

    single {
        object : JvmDatabaseConfiguration {
            override val databaseName: String = config.databaseName
            override val databasePath: String = config.databasePath.ifEmpty {
                defaultAppDirectory(config.databaseName)
            }
        }
    } binds arrayOf(DatabaseConfiguration::class, JvmDatabaseConfiguration::class)
}