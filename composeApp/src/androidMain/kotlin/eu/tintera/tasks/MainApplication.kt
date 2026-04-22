package eu.tintera.tasks

import android.app.Application
import org.koin.android.ext.koin.androidContext

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        val app = koinApp {
            androidContext(this@MainApplication)
        }

        TaskManagerInitializer.initialize(
            context = this,
            config = AndroidTasksConfiguration(
                compatTransformation = {
                    it.toByteArray()
                }
            ),
            taskLifecycleObservers = app.koin.getAll()
        )
    }
}