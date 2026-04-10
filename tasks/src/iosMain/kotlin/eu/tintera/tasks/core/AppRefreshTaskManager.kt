package eu.tintera.tasks.core

import eu.tintera.tasks.core.data.Repository
import eu.tintera.tasks.core.data.Task
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import platform.BackgroundTasks.BGAppRefreshTaskRequest

internal class AppRefreshTaskManager(
    scope: ApplicationScope,
    dispatchers: AppDispatchers,
    private val taskIdentifier: String,
    repository: Repository,
    appLifecycleObserver: AppLifecycleObserver
) : BgTaskManager(
    scope = scope,
    dispatchers = dispatchers,
    taskIdentifier = taskIdentifier,
    repository = repository,
    appLifecycleObserver = appLifecycleObserver,
    tag = "AppRefreshTaskManager"
), ExecutionCapabilityProvider {
    override fun List<Task>.filter(): List<Task> = filterNot {
        it.requiresDeviceIdle
    }

    override fun createRequest() = BGAppRefreshTaskRequest(taskIdentifier)
    override fun capabilities(): Flow<Set<ExecutionCapability>> = currentToken.map {
        setOfNotNull(it?.let { ExecutionCapability.SHORT_LIVED })
    }

    companion object {
        private const val TAG = "AppRefreshTaskManager"
    }
}