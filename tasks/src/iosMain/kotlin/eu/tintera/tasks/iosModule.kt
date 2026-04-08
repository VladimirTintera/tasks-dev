package eu.tintera.tasks

import androidx.sqlite.SQLiteDriver
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import eu.tintera.tasks.core.*
import eu.tintera.tasks.core.locks.*
import eu.tintera.tasks.db.DatabaseConfiguration
import eu.tintera.tasks.db.databaseModule
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
            BgTaskManager(
                appPackage = identifier,
                repository = get(),
                appLifecycleObserver = get()
            )
        } binds arrayOf(BgTaskManager::class, TokenProducer::class, ExecutionContextObserver::class)
    }

    single {
        CompositeExecutionContextObserver(getAll())
    }

    singleOf(::IosExecutionContextProvider) bind ExecutionContextProvider::class

    single {
        UiBackgroundTaskTokenProducer(
            scope = get<ApplicationScope>(),
            dispatcher = get<AppDispatchers>().default,
            appLifecycleObserver = get()
        )
    } bind TokenProducer::class


    singleOf(::AppLifecycleObserver)

    single {
        IosTokenProvider(
            scope = get(),
            dispatchers = get(),
            tokenProducers = getAll()
        )
    }
}