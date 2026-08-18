package eu.tintera.background.tasks.runtime

import androidx.sqlite.SQLiteDriver
import androidx.sqlite.driver.AndroidSQLiteDriver
import eu.tintera.background.guard.ExecutionEnvironmentConfig
import eu.tintera.background.guard.PlatformContext
import eu.tintera.background.tasks.TaskManagerConfiguration
import eu.tintera.background.tasks.android.WorkManagerConfiguration
import eu.tintera.background.tasks.android.androidModule
import eu.tintera.background.tasks.android.db.androidDbModule
import eu.tintera.background.tasks.core.guard.guardInit
import eu.tintera.background.tasks.db.DatabaseConfiguration
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
            override val databaseDirectory: String? = config.databaseDirectory
            override val allowDestructiveMigration: Boolean = config.allowDestructiveMigration
        }
    }

    single { PlatformContext(get()) }
}