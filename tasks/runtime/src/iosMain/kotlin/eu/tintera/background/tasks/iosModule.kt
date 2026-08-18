package eu.tintera.background.tasks

import androidx.sqlite.SQLiteDriver
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import eu.tintera.background.guard.ExecutionContextObserver
import eu.tintera.background.guard.ExecutionEnvironmentConfig
import eu.tintera.background.guard.PlatformContext
import eu.tintera.background.tasks.core.TaskProcessorConfig
import eu.tintera.background.tasks.core.cleanup.DatabaseCleanupService
import eu.tintera.background.tasks.core.guard.guardInit
import eu.tintera.background.tasks.db.DatabaseConfiguration
import eu.tintera.background.tasks.ios.db.iosDbModule
import eu.tintera.background.tasks.ios.iosModule
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

internal fun iosModule(
    config: TaskManagerConfiguration
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

    single { PlatformContext() }

    single {
        DatabaseCleanupService {}
    } bind DatabaseCleanupService::class

    single<SQLiteDriver> { config.sqLiteDriver ?: BundledSQLiteDriver() }

    single<DatabaseConfiguration> {
        object : DatabaseConfiguration {
            override val databaseName: String = config.databaseName
            override val databaseDirectory: String? = config.databaseDirectory
            override val allowDestructiveMigration: Boolean = config.allowDestructiveMigration
        }
    }
}