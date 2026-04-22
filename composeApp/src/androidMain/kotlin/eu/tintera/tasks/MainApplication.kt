package eu.tintera.tasks

import android.app.Application
import org.koin.android.ext.koin.androidContext

class MainApplication : Application(), TaskManagerConfigProvider {
    override fun onCreate() {
        super.onCreate()

        koinApp {
            androidContext(this@MainApplication)
        }
    }

    override val tasksManagerConfig: AndroidTasksConfiguration = AndroidTasksConfiguration(
        compatTransformation = {
            it.toByteArray()
        }
    )
}