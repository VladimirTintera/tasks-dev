package eu.tintera.tasks

import android.content.Context
import androidx.startup.Initializer
import androidx.work.WorkManagerInitializer
import eu.tintera.tasks.koin.startTasksKoin
import org.koin.android.ext.koin.androidContext

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
            modules(androidModule(config))
        }
    }


    override fun dependencies(): MutableList<Class<out Initializer<*>>> {
        return mutableListOf(WorkManagerInitializer::class.java)
    }
}