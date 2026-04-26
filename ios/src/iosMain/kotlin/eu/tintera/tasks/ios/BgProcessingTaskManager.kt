package eu.tintera.tasks.ios

import eu.tintera.tasks.core.AppDispatchers
import eu.tintera.tasks.core.ApplicationScope
import eu.tintera.tasks.core.ProcessableTask
import eu.tintera.tasks.core.constraints.PreconditionResult
import eu.tintera.tasks.core.constraints.Constraint
import kotlinx.coroutines.flow.map
import platform.BackgroundTasks.BGProcessingTaskRequest
import kotlin.time.Clock

internal class BgProcessingTaskManager(
    scope: ApplicationScope,
    dispatchers: AppDispatchers,
    private val taskIdentifier: String,
    repository: BgTaskManagerRepository,
    appLifecycleObserver: AppLifecycleObserver,
    private val isAppRefreshTaskAllowed: Boolean,
    clock: Clock
) : BgTaskManager(
    scope = scope,
    dispatchers = dispatchers,
    taskIdentifier = taskIdentifier,
    repository = repository,
    appLifecycleObserver = appLifecycleObserver,
    tag = "BgProcessingTaskManager",
    clock = clock,
), Constraint {

    override fun List<BgTaskManagerTask>.filter() = if (!isAppRefreshTaskAllowed) this else filter {
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