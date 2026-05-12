package eu.tintera.tasks

import androidx.sqlite.SQLiteDriver
import eu.tintera.guard.ExecutionEnvironmentConfig
import eu.tintera.guard.PlatformContext
import eu.tintera.tasks.core.AppStateObserver
import eu.tintera.tasks.core.NetworkState
import eu.tintera.tasks.core.TaskProcessorConfig
import eu.tintera.tasks.core.cleanup.DatabaseCleanupService
import eu.tintera.tasks.core.engineModule
import eu.tintera.tasks.core.guard.guardInit
import eu.tintera.tasks.db.DatabaseConfiguration
import eu.tintera.tasks.engine.db.engineDbModule
import eu.tintera.tasks.runtime.WebAppStateObserver
import eu.tintera.tasks.runtime.WebNetworkState
import eu.tintera.tasks.runtime.createSQLiteWasmWorker
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

internal fun webModule(
    config: TaskManagerConfiguration
) = module {

    includes(
        engineModule,
        engineDbModule,
    )

    singleOf(::WebAppStateObserver) bind AppStateObserver::class
    singleOf(::WebNetworkState) bind NetworkState::class

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

    single {
        DatabaseCleanupService {}
    } bind DatabaseCleanupService::class

    single<SQLiteDriver> { config.sqLiteDriver ?: createSQLiteWasmWorker() }

    single<DatabaseConfiguration> {
        object : DatabaseConfiguration {
            override val databaseName: String = config.databaseName
        }
    }
}