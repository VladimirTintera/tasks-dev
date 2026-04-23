package eu.tintera.tasks.runtime

import eu.tintera.tasks.core.AppDispatchers
import eu.tintera.tasks.core.ApplicationScope
import eu.tintera.tasks.core.data.ProcessableTask
import eu.tintera.tasks.core.data.Repository
import eu.tintera.tasks.core.data.SchedulableTask
import eu.tintera.tasks.core.preconditions.PreconditionResult
import eu.tintera.tasks.core.preconditions.TaskPrecondition
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
), TaskPrecondition {

    override fun List<SchedulableTask>.filter() = if (!isAppRefreshTaskAllowed) this else filter {
        it.requiresDeviceIdle
    }

    override fun createRequest() = BGProcessingTaskRequest(taskIdentifier).apply {
        requiresExternalPower = false
        requiresNetworkConnectivity = lastKnownTasks.any { it.networkRequired }
    }

    override fun hasConstraint(task: ProcessableTask) = task.requiresDeviceIdle

    override fun isValid(task: ProcessableTask) = currentToken.map {
        if (it != null) PreconditionResult.Met else PreconditionResult.Unmet
    }

    override val monitorDuringExecution = true
}