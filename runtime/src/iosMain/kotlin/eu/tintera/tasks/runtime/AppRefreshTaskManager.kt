package eu.tintera.tasks.runtime

import eu.tintera.tasks.core.AppDispatchers
import eu.tintera.tasks.core.ApplicationScope
import eu.tintera.tasks.core.data.Repository
import eu.tintera.tasks.core.data.SchedulableTask
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
) {
    override fun List<SchedulableTask>.filter(): List<SchedulableTask> = filterNot {
        it.requiresDeviceIdle
    }

    override fun createRequest() = BGAppRefreshTaskRequest(taskIdentifier)

    companion object {
        private const val TAG = "AppRefreshTaskManager"
    }
}