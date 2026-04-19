package eu.tintera.tasks

import androidx.sqlite.SQLiteDriver
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.work.WorkManager
import eu.tintera.tasks.core.WorkManagerCoreTaskManager
import eu.tintera.guard.ExecutionContextConfig
import eu.tintera.tasks.core.CoreTaskManager
import eu.tintera.tasks.core.coreModule
import eu.tintera.tasks.db.DatabaseConfiguration
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module

internal fun androidModule(
    config: AndroidTasksConfiguration
): Module = module {

    includes(coreModule)

    factory<WorkManager> { WorkManager.getInstance(get()) }
    factoryOf(::WorkManagerCoreTaskManager) bind CoreTaskManager::class

    single {
        ExecutionContextConfig(
            releaseDebounce = config.executionContextReleaseDebounce
        )
    }

    single {
        WorkManagerConfiguration(compatTransformation = config.compatTransformation)
    }

    single<SQLiteDriver> { AndroidSQLiteDriver() }

    single<DatabaseConfiguration> {
        object : DatabaseConfiguration {
            override val databaseName: String = ""
        }
    }
}