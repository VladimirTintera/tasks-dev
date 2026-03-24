package eu.tintera.tasks

import android.content.Context
import androidx.startup.Initializer
import androidx.work.Configuration
import androidx.work.WorkManager
import androidx.work.WorkManagerInitializer
import eu.tintera.tasks.core.TaskProcessorConfig
import eu.tintera.tasks.core.locks.ExecutionContextConfig
import eu.tintera.tasks.koin.startTasksKoin
import org.koin.android.ext.koin.androidContext
import org.koin.core.KoinApplication
import org.koin.dsl.module
import java.util.concurrent.Executors
import kotlin.math.sin

class TasksInitializer : Initializer<KoinApplication> {
    override fun create(
        context: Context
    ): KoinApplication {

        val config = if (context is TaskManagerConfigProvider)
            context.taskManagerConfig
        else
            TaskManagerConfig()

        // Vytvoříme thread pool s přesně takovým limitem, jaký si vývojář nastavil v maxConcurrentTasks
        //val customExecutor = Executors.newFixedThreadPool(config.maxConcurrentTasks)

        //val workManagerConfig = Configuration.Builder()
        //    .setExecutor(customExecutor)
        //    .build()

        // Manuální inicializace WorkManageru
        //WorkManager.initialize(context, workManagerConfig)

        return startTasksKoin {
            androidContext(context)
            modules(
                module {
                    single { config }
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
                }
            )
        }
    }


    override fun dependencies(): MutableList<Class<out Initializer<*>>> {
        return mutableListOf(WorkManagerInitializer::class.java)
    }
}