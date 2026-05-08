package eu.tintera.tasks

import android.app.Application
import eu.tintera.tasks.koin.taskManagerBootstrapper
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.loadKoinModules
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
                        TaskManagerConfiguration(
                            context = get(),
                            compatTransformation = {
                                it.toByteArray()
                            }
                        )
                    }

                    taskManagerBootstrapper {
                        koin.loadModules(listOf(logModule), createEagerInstances = true)
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