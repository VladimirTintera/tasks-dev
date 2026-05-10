package eu.tintera.tasks.ios

import eu.tintera.tasks.core.AppDispatchers
import eu.tintera.tasks.core.ApplicationScope
import platform.BackgroundTasks.BGAppRefreshTaskRequest
import kotlin.time.Clock

internal class AppRefreshTaskManager(
    scope: ApplicationScope,
    dispatchers: AppDispatchers,
    private val taskIdentifier: String,
    repository: BgTaskManagerRepository,
    appLifecycleObserver: AppLifecycleObserver,
    clock: Clock
) : BgTaskManager(
    scope = scope,
    dispatchers = dispatchers,
    taskIdentifier = taskIdentifier,
    repository = repository,
    appLifecycleObserver = appLifecycleObserver,
    tag = "AppRefreshTaskManager",
    clock = clock,
) {
    override fun List<BgTaskManagerTask>.filter() = filterNot {
        it.requiresDeviceIdle
    }

    override fun createRequest() = BGAppRefreshTaskRequest(taskIdentifier)
}