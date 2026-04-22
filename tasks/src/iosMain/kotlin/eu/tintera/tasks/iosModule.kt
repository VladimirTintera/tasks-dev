package eu.tintera.tasks

import androidx.sqlite.SQLiteDriver
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import eu.tintera.guard.*
import eu.tintera.tasks.core.*
import eu.tintera.tasks.core.cleanup.DatabaseCleanupPolicy
import eu.tintera.tasks.core.guard.guardInit
import eu.tintera.tasks.core.preconditions.TaskPrecondition
import eu.tintera.tasks.db.DatabaseConfiguration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.plus
import org.koin.core.module.Module
import org.koin.core.module.dsl.createdAtStart
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.binds
import org.koin.dsl.module

internal fun iosModule(
    config: IosTasksManagerConfiguration
): Module = module {

    includes(
        engineModule(DatabaseCleanupPolicy.DISABLED_GHOST_TASKS_POLICY)
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

    single<SQLiteDriver> { config.sqLiteDriver ?: BundledSQLiteDriver() }

    single<DatabaseConfiguration> {
        object : DatabaseConfiguration {
            override val databaseName: String = config.databaseName
        }
    }

    factoryOf(::RepositoryCoreTaskManager) bind CoreTaskManager::class


    singleOf<NetworkState>(::IosNetworkState)

    config.bgProcessingTaskIdentifier?.also { identifier ->
        single(createdAtStart = true) {
            BgProcessingTaskManager(
                scope = get(),
                dispatchers = get(),
                taskIdentifier = identifier,
                repository = get(),
                appLifecycleObserver = get(),
                isAppRefreshTaskAllowed = config.appRefreshTaskIdentifier != null
            )
        } binds arrayOf(TokenProducer::class, ExecutionContextObserver::class, TaskPrecondition::class)
    }

    config.appRefreshTaskIdentifier?.also { identifier ->
        single(createdAtStart = true) {
            AppRefreshTaskManager(
                scope = get(),
                dispatchers = get(),
                taskIdentifier = identifier,
                repository = get(),
                appLifecycleObserver = get()
            )
        } binds arrayOf(TokenProducer::class, ExecutionContextObserver::class)
    }




    singleOf(::AppLifecycleObserver) {
        createdAtStart()
    } bind AppStateObserver::class

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