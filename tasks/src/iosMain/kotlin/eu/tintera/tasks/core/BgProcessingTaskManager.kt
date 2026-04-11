package eu.tintera.tasks.core

import eu.tintera.tasks.core.data.Repository
import eu.tintera.tasks.core.data.Task
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import platform.BackgroundTasks.BGProcessingTaskRequest

internal class BgProcessingTaskManager(
    scope: ApplicationScope,
    dispatchers: AppDispatchers,
    private val taskIdentifier: String,
    repository: Repository,
    appLifecycleObserver: AppLifecycleObserver,
    private val isAppRefreshTaskAllowed: Boolean
) : BgTaskManager(
    scope = scope,
    dispatchers = dispatchers,
    taskIdentifier = taskIdentifier,
    repository = repository,
    appLifecycleObserver = appLifecycleObserver,
    tag = "BgProcessingTaskManager"
), ExecutionWindowProvider, TaskPrecondition {

    override fun List<Task>.filter(): List<Task> = if (!isAppRefreshTaskAllowed) this else filter {
        it.requiresDeviceIdle
    }

    override fun createRequest() = BGProcessingTaskRequest(taskIdentifier).apply {
        requiresExternalPower = false
        requiresNetworkConnectivity = true
    }

    override fun capabilities(): Flow<Set<ExecutionWindo>> = currentToken.map {
        setOfNotNull(it?.let { ExecutionWindo.LONG })
    }

    override fun hasConstraint(task: Task) = task.requiresDeviceIdle

    override fun isValid(task: Task) = currentToken.map { it != null }

    override val monitorDuringExecution = true
}