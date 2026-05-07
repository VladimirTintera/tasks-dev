package eu.tintera.tasks

import android.app.Application
import eu.tintera.guard.Token
import org.koin.android.ext.koin.androidContext

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        val app = koinApp {
            androidContext(this@MainApplication)
        }

        TasksInitializer.initialize(
            config = AndroidTasksConfiguration(
                context = this,
                compatTransformation = {
                    it.toByteArray()
                }
            ),
            taskLifecycleObservers = app.koin.getAll()
        )

        app.koin.get<TokenObserver>().start()
    }
}