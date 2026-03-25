package eu.tintera.tasks

import androidx.sqlite.SQLiteDriver
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import eu.tintera.tasks.core.TaskProcessorConfig
import eu.tintera.tasks.core.locks.ExecutionContextConfig
import eu.tintera.tasks.db.DatabaseConfiguration
import eu.tintera.tasks.db.JvmDatabaseConfiguration
import eu.tintera.tasks.koin.startTasksKoin
import org.koin.dsl.bind
import org.koin.dsl.binds
import org.koin.dsl.module

object TasksInitializer {

    fun initialize(
        config: JvmTasksManagerConfiguration
    ) {
        startTasksKoin {
            modules(
                module {
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

                    single<SQLiteDriver> { BundledSQLiteDriver() }

                    single {
                        object : JvmDatabaseConfiguration {
                            override val databaseName: String = config.databaseName
                            override val databasePath: String = config.databasePath.ifEmpty {
                                defaultAppDirectory(config.databaseName)
                            }
                        }
                    } binds arrayOf(DatabaseConfiguration::class, JvmDatabaseConfiguration::class)
                }
            )
        }
    }
}