package eu.tintera.tasks

import androidx.sqlite.SQLiteDriver
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import eu.tintera.guard.ExecutionEnvironmentConfig
import eu.tintera.guard.ExecutionContextProvider
import eu.tintera.guard.PlatformContext
import eu.tintera.guard.TokenProvider
import eu.tintera.tasks.core.*
import eu.tintera.tasks.core.cleanup.DatabaseCleanupPolicy
import eu.tintera.tasks.core.guard.guardInit
import eu.tintera.tasks.db.DatabaseConfiguration
import eu.tintera.tasks.db.JvmDatabaseConfiguration
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.binds
import org.koin.dsl.module

internal fun jvmModule(
    config: JvmTasksManagerConfiguration
): Module = module {

    includes(engineModule(DatabaseCleanupPolicy.DISABLED_GHOST_TASKS_POLICY))

    factoryOf(::RepositoryCoreTaskManager) bind CoreTaskManager::class

    singleOf<NetworkState>(::JvmNetworkState)

    singleOf(::JvmTokenProvider) bind TokenProvider::class

    singleOf(::JvmAppStateObserver) bind AppStateObserver::class

    single {
        TaskProcessorConfig(
            maxConcurrentTasks = config.maxConcurrentTasks
        )
    }

    guardInit(
        executionEnvironment = config.executionEnvironment,
        platformContext = PlatformContext(),
        config = ExecutionEnvironmentConfig(
            releaseDebounce = config.executionContextReleaseDebounce
        )
    )

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