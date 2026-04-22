package eu.tintera.tasks

import android.content.Context
import androidx.startup.Initializer
import androidx.work.WorkManagerInitializer

class TaskManagerStartupInitializer : Initializer<Unit> {
    override fun create(
        context: Context
    ) {

        TaskManagerInitializer.initialize(
            context = context,
            config = AndroidTasksConfiguration()
        )
    }


    override fun dependencies(): MutableList<Class<out Initializer<*>>> {
        return mutableListOf(WorkManagerInitializer::class.java)
    }
}