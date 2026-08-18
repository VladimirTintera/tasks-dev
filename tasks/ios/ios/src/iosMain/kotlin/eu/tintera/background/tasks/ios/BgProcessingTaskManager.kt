package eu.tintera.background.tasks.ios

import eu.tintera.background.tasks.core.AppDispatchers
import eu.tintera.background.tasks.core.ApplicationScope
import eu.tintera.background.tasks.core.CompositeTasksLogger
import eu.tintera.background.tasks.core.ProcessableTask
import eu.tintera.background.tasks.core.constraints.ConstraintResult
import eu.tintera.background.tasks.core.constraints.Constraint
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
    clock: Clock,
    log: CompositeTasksLogger
) : BgTaskManager(
    scope = scope,
    dispatchers = dispatchers,
    taskIdentifier = taskIdentifier,
    repository = repository,
    appLifecycleObserver = appLifecycleObserver,
    tag = "BgProcessingTaskManager",
    clock = clock,
    log = log,
), Constraint {

    override fun List<BgTaskManagerTask>.filter() = if (!isAppRefreshTaskAllowed) this else filter {
        it.requiresDeviceIdle
    }

    override fun createRequest() = BGProcessingTaskRequest(taskIdentifier).apply {
        requiresExternalPower = false
        requiresNetworkConnectivity = lastKnownTasks.any { it.networkRequired }
    }

    override fun hasConstraint(task: ProcessableTask) = if (isAppRefreshTaskAllowed) task.requiresDeviceIdle else false

    override fun isValid(task: ProcessableTask) = pendingToken.map {
        if (it.isNotEmpty()) ConstraintResult.Met else ConstraintResult.Unmet
    }

    override val monitorDuringExecution = true
}