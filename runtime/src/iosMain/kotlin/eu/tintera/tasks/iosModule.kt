package eu.tintera.tasks

import androidx.sqlite.SQLiteDriver
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import eu.tintera.guard.ExecutionContextObserver
import eu.tintera.guard.ExecutionEnvironmentConfig
import eu.tintera.guard.PlatformContext
import eu.tintera.tasks.core.TaskProcessorConfig
import eu.tintera.tasks.core.cleanup.DatabaseCleanupService
import eu.tintera.tasks.core.guard.guardInit
import eu.tintera.tasks.db.DatabaseConfiguration
import eu.tintera.tasks.ios.db.iosDbModule
import eu.tintera.tasks.ios.iosModule
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

internal fun iosModule(
    config: IosTasksManagerConfiguration
): Module = module {

    includes(
        iosModule(
            bgProcessingTaskIdentifier = config.bgProcessingTaskIdentifier,
            appRefreshTaskIdentifier = config.appRefreshTaskIdentifier,
        ),
        iosDbModule
    )

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

    single {
        DatabaseCleanupService {}
    } bind DatabaseCleanupService::class

    single<SQLiteDriver> { config.sqLiteDriver ?: BundledSQLiteDriver() }

    single<DatabaseConfiguration> {
        object : DatabaseConfiguration {
            override val databaseName: String = config.databaseName
        }
    }

    singleOf(::DebugObserver) bind ExecutionContextObserver::class
}

class DebugObserver : ExecutionContextObserver {
    override fun onPreCancel() {
        EventBus.send(TAG, "nnPreCancel")
    }

    override suspend fun onPreRelease() {
        EventBus.send(TAG, "onPreRelease")
    }

    override fun onStarted() {
        EventBus.send(TAG, "onStarted")
    }

    companion object {
        private const val TAG = "DebugObserver"
    }
}