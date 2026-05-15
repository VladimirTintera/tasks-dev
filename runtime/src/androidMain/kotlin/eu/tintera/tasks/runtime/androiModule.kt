package eu.tintera.tasks.runtime

import androidx.sqlite.SQLiteDriver
import androidx.sqlite.driver.AndroidSQLiteDriver
import eu.tintera.guard.ExecutionEnvironmentConfig
import eu.tintera.guard.PlatformContext
import eu.tintera.tasks.TaskManagerConfiguration
import eu.tintera.tasks.android.WorkManagerConfiguration
import eu.tintera.tasks.android.androidModule
import eu.tintera.tasks.android.db.androidDbModule
import eu.tintera.tasks.core.guard.guardInit
import eu.tintera.tasks.db.DatabaseConfiguration
import org.koin.core.module.Module
import org.koin.dsl.module

internal fun androidModule(
    config: TaskManagerConfiguration
): Module = module {

    includes(
        androidModule,
        androidDbModule
    )
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

    single { PlatformContext(get()) }
}