package eu.tintera.background.tasks

import androidx.sqlite.SQLiteDriver
import eu.tintera.background.guard.ExecutionEnvironmentConfig
import eu.tintera.background.guard.PlatformContext
import eu.tintera.background.tasks.core.AppStateObserver
import eu.tintera.background.tasks.core.NetworkState
import eu.tintera.background.tasks.core.TaskProcessorConfig
import eu.tintera.background.tasks.core.cleanup.DatabaseCleanupService
import eu.tintera.background.tasks.core.engineModule
import eu.tintera.background.tasks.core.guard.guardInit
import eu.tintera.background.tasks.db.DatabaseConfiguration
import eu.tintera.background.tasks.engine.db.engineDbModule
import eu.tintera.background.tasks.runtime.WebAppStateObserver
import eu.tintera.background.tasks.runtime.WebNetworkState
import eu.tintera.background.tasks.web.sqliteDriver
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

    single {
        DatabaseCleanupService {}
    } bind DatabaseCleanupService::class

    single<SQLiteDriver> { config.sqLiteDriver ?: sqliteDriver() }

    single<DatabaseConfiguration> {
        object : DatabaseConfiguration {
            override val databaseName: String = config.databaseName
        }
    }
}