package eu.tintera.tasks

import eu.tintera.tasks.koin.TasksKoinContext
import eu.tintera.tasks.koin.mainModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import org.koin.core.KoinApplication
import org.koin.dsl.bind
import org.koin.dsl.koinApplication
import org.koin.dsl.module

object TaskManagerBootstrapper {
    // Reaktivní stav inicializace. Výchozí je null (spíme).
    private val _taskManager = MutableStateFlow<TaskManager?>(null)

    // Vystavíme ven pouze pro čtení
    val taskManager = _taskManager.asStateFlow()

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

        _taskManager.value = TaskManager.getInstance()
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