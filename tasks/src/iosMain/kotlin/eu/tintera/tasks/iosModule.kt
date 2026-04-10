package eu.tintera.tasks

import androidx.sqlite.SQLiteDriver
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import eu.tintera.guard.ExecutionContextConfig
import eu.tintera.guard.ExecutionContextObserver
import eu.tintera.guard.ExecutionContextProvider
import eu.tintera.guard.ExecutionEnvironmentFactory
import eu.tintera.guard.PlatformContext
import eu.tintera.guard.TokenProducer
import eu.tintera.tasks.core.*
import eu.tintera.tasks.db.DatabaseConfiguration
import eu.tintera.tasks.db.databaseModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.plus
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.binds
import org.koin.dsl.module

internal fun iosModule(
    config: IosTasksManagerConfiguration
): Module = module {

    includes(databaseModule, coreModule)

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

    single<SQLiteDriver> { config.sqLiteDriver ?: BundledSQLiteDriver() }

    single<DatabaseConfiguration> {
        object : DatabaseConfiguration {
            override val databaseName: String = config.databaseName
        }
    }

    factoryOf(::RepositoryTaskScopeFactory) bind TaskScopeFactory::class

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
        } binds arrayOf(TokenProducer::class, ExecutionContextObserver::class)
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


    single {
        ExecutionEnvironmentFactory.createDefault(
            context = PlatformContext(),
            scope = get<ApplicationScope>() + Dispatchers.Default,
            config = get(),
            tokenProducers = getAll(),
            observers = getAll()
        )
    } bind ExecutionContextProvider::class

    singleOf(::AppLifecycleObserver)
}