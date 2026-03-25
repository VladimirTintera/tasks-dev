package eu.tintera.tasks

import android.content.Context
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.startup.Initializer
import androidx.work.WorkManagerInitializer
import eu.tintera.tasks.core.TaskProcessorConfig
import eu.tintera.tasks.core.locks.ExecutionContextConfig
import eu.tintera.tasks.db.DatabaseConfiguration
import eu.tintera.tasks.koin.startTasksKoin
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

class TasksInitializer : Initializer<Unit> {
    override fun create(
        context: Context
    ) {

        val config = if (context is TaskManagerConfigProvider)
            context.tasksManagerConfig
        else
            AndroidTasksConfiguration()

        // Vytvoříme thread pool s přesně takovým limitem, jaký si vývojář nastavil v maxConcurrentTasks
        //val customExecutor = Executors.newFixedThreadPool(config.maxConcurrentTasks)

        //val workManagerConfig = Configuration.Builder()
        //    .setExecutor(customExecutor)
        //    .build()

        // Manuální inicializace WorkManageru
        //WorkManager.initialize(context, workManagerConfig)

        startTasksKoin {
            androidContext(context)
            modules(
                module {
                    single {
                        TaskProcessorConfig(
                            maxConcurrentTasks = 10
                        )
                    }
                    single {
                        ExecutionContextConfig(
                            releaseDebounce = config.executionContextReleaseDebounce
                        )
                    }

                    single<SQLiteDriver> { AndroidSQLiteDriver() }

                    single<DatabaseConfiguration> {
                        object : DatabaseConfiguration {
                            override val databaseName: String = ""
                        }
                    }
                }
            )
        }
    }


    override fun dependencies(): MutableList<Class<out Initializer<*>>> {
        return mutableListOf(WorkManagerInitializer::class.java)
    }
}