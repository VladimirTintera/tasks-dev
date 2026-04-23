package eu.tintera.tasks.runtime

import androidx.sqlite.SQLiteDriver
import androidx.sqlite.driver.AndroidSQLiteDriver
import eu.tintera.guard.ExecutionEnvironmentConfig
import eu.tintera.tasks.AndroidTasksConfiguration
import eu.tintera.tasks.android.WorkManagerConfiguration
import eu.tintera.tasks.android.androidModule
import eu.tintera.tasks.db.DatabaseConfiguration
import org.koin.core.module.Module
import org.koin.dsl.module

internal fun androidModule(
    config: AndroidTasksConfiguration
): Module = module {

    includes(androidModule)
    single {
        ExecutionEnvironmentConfig(
            releaseDebounce = config.executionContextReleaseDebounce
        )
    }

    single {
        WorkManagerConfiguration(compatTransformation = config.compatTransformation)
    }

    single<SQLiteDriver> { config.sqLiteDriver ?: AndroidSQLiteDriver() }

    single<DatabaseConfiguration> {
        object : DatabaseConfiguration {
            override val databaseName: String = config.databaseName
        }
    }
}