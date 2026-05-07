package eu.tintera.tasks.runtime

import androidx.sqlite.SQLiteDriver
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import eu.tintera.guard.ExecutionEnvironmentConfig
import eu.tintera.guard.PlatformContext
import eu.tintera.guard.TokenProvider
import eu.tintera.tasks.JvmTasksManagerConfiguration
import eu.tintera.tasks.core.AppStateObserver
import eu.tintera.tasks.core.NetworkState
import eu.tintera.tasks.core.TaskProcessorConfig
import eu.tintera.tasks.core.cleanup.DatabaseCleanupService
import eu.tintera.tasks.core.engineModule
import eu.tintera.tasks.core.guard.guardInit
import eu.tintera.tasks.db.DatabaseConfiguration
import eu.tintera.tasks.db.JvmDatabaseConfiguration
import eu.tintera.tasks.defaultAppDirectory
import eu.tintera.tasks.engine.db.engineDbModule
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.binds
import org.koin.dsl.module

internal fun jvmModule(
    config: JvmTasksManagerConfiguration
): Module = module {

    includes(
        engineModule,
        engineDbModule,
    )

    single {
        DatabaseCleanupService {}
    } bind DatabaseCleanupService::class

    singleOf<NetworkState>(::JvmNetworkState)

    singleOf(::JvmTokenProvider) bind TokenProvider::class

    singleOf(::JvmAppStateObserver) bind AppStateObserver::class

    single {
        TaskProcessorConfig(
            maxConcurrentTasks = config.maxConcurrentTasks
        )
    }

    single { PlatformContext() }

    guardInit(
        executionEnvironment = config.executionEnvironment,
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