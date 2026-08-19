package eu.tintera.background.tasks.di

import eu.tintera.background.tasks.InternalTasksApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import org.koin.core.KoinApplication

@InternalTasksApi
object TasksKoinContext {

    private val application = MutableStateFlow<KoinApplication?>(null)

    var koinApp: KoinApplication
        get() = application.value ?: error(
            "TaskManager is not initialized yet. Call TasksInitializer.initialize(...) first."
        )
        set(value) {
            application.value = value
        }

    /**
     * Suspends until the library is initialized.
     *
     * Exists for entry points the **system** can trigger before the application had a chance to
     * initialize anything — on Android, WorkManager starts itself and its scheduler may reach for
     * work while `Application.onCreate` is still running. Waiting costs milliseconds; failing and
     * relying on a retry would push the same work minutes into the future for no reason.
     */
    suspend fun awaitKoinApp(): KoinApplication = application.filterNotNull().first()
}
