package eu.tintera.tasks

import androidx.sqlite.SQLiteDriver
import androidx.sqlite.driver.NativeSQLiteDriver
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import eu.tintera.tasks.core.TaskProcessorConfig
import eu.tintera.tasks.core.locks.ExecutionContextConfig
import eu.tintera.tasks.db.DatabaseConfiguration
import eu.tintera.tasks.koin.startTasksKoin
import org.koin.dsl.module

object TasksInitializer {
    fun initialize(
        config: IosTasksManagerConfiguration = IosTasksManagerConfiguration()
    ) {
        startTasksKoin {
            modules(
                iosModule(config),
            )
        }
    }
}