package eu.tintera.background.tasks.ios

import eu.tintera.background.tasks.core.AppDispatchers
import eu.tintera.background.tasks.core.ApplicationScope
import eu.tintera.background.tasks.core.CompositeTasksLogger
import platform.BackgroundTasks.BGAppRefreshTaskRequest
import kotlin.time.Clock

internal class AppRefreshTaskManager(
    scope: ApplicationScope,
    dispatchers: AppDispatchers,
    private val taskIdentifier: String,
    repository: BgTaskManagerRepository,
    appLifecycleObserver: AppLifecycleObserver,
    clock: Clock,
    log: CompositeTasksLogger
) : BgTaskManager(
    scope = scope,
    dispatchers = dispatchers,
    taskIdentifier = taskIdentifier,
    repository = repository,
    appLifecycleObserver = appLifecycleObserver,
    tag = "AppRefreshTaskManager",
    clock = clock,
    log = log,
) {
    override fun List<BgTaskManagerTask>.filter() = filterNot {
        it.requiresDeviceIdle
    }

    override fun createRequest() = BGAppRefreshTaskRequest(taskIdentifier)
}