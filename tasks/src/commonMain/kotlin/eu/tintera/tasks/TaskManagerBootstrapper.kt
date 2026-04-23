package eu.tintera.tasks

import eu.tintera.tasks.koin.TasksKoinContext
import eu.tintera.tasks.koin.mainModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import org.koin.core.KoinApplication
import org.koin.dsl.bind
import org.koin.dsl.koinApplication
import org.koin.dsl.module

internal object TaskManagerBootstrapper {

    private val initialized = MutableStateFlow(false)

    internal fun initialize(
        taskLifecycleObservers: List<TaskLifecycleObserver>,
        koinAppInitialization: KoinApplication.() -> Unit = {}
    ) {
        if (initialized.getAndUpdate { true }) return

        startTasksKoin(
            taskLifecycleObservers = taskLifecycleObservers,
            koinAppInitialization = koinAppInitialization
        )
    }

    private fun startTasksKoin(
        taskLifecycleObservers: List<TaskLifecycleObserver>,
        koinAppInitialization: KoinApplication.() -> Unit = {}
    ): KoinApplication {
        TasksKoinContext.koinApp = koinApplication {
            koinAppInitialization()
            modules(
                mainModule(),
            )
            modules(
                taskLifecycleObservers.map { observer ->
                    module {
                        single { observer } bind TaskLifecycleObserver::class
                    }
                }
            )
        }

        return TasksKoinContext.koinApp
    }
}