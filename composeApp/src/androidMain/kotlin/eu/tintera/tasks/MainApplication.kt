package eu.tintera.tasks

import android.app.Application
import eu.tintera.tasks.koin.taskManagerBootstrapper
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        val app = koinApp {
            androidContext(this@MainApplication)
            modules(
                module {
                    single {
                        AndroidTasksConfiguration(
                            context = get(),
                            compatTransformation = {
                                it.toByteArray()
                            }
                        )
                    }

                    taskManagerBootstrapper {
                        get<TokenObserver>().start()
                    }
                }
            )
        }

        /*TasksInitializer.initialize(
            config = AndroidTasksConfiguration(
                context = this,
                compatTransformation = {
                    it.toByteArray()
                }
            ),
            taskLifecycleObservers = app.koin.getAll()
        )*/

        //app.koin.get<TokenObserver>().start()
    }
}