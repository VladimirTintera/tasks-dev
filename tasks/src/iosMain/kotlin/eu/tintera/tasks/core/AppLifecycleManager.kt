package eu.tintera.tasks.core

import eu.tintera.tasks.State
import eu.tintera.tasks.core.data.Repository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationDidEnterBackgroundNotification
import platform.UIKit.UIApplicationState
import platform.UIKit.UIApplicationWillEnterForegroundNotification

/**
 * Manages the application lifecycle events on iOS and coordinates task recovery.
 *
 * This class listens for UIKit lifecycle notifications to track background/foreground states
 * and ensures that tasks stuck in a [State.Running] state are reset when the app resumes.
 */
internal class AppLifecycleManager(
    private val repository: Repository,
    private val bgTaskManager: BgTaskManager,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val isBackground = MutableStateFlow(
        UIApplication.sharedApplication.applicationState == UIApplicationState.UIApplicationStateBackground
    )

    init {
        setupLifecycleObservers()
        recoverStuckTasks()
    }

    fun createExpirationToken(
        onExpire: () -> Unit
    ) = LifecycleToken(
        scope = scope,
        expirationHandler = onExpire,
        isBackground = isBackground,
        bgTaskManager = bgTaskManager,
    )

    private fun setupLifecycleObservers() {
        val center = NSNotificationCenter.defaultCenter

        // 2. Úklid při návratu ze zmražení do popředí (Foreground)
        center.addObserverForName(
            name = UIApplicationWillEnterForegroundNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue
        ) { _ ->
            isBackground.update { false }
            recoverStuckTasks()
        }

        center.addObserverForName(
            name = UIApplicationDidEnterBackgroundNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue
        ) { _ ->
            isBackground.update { true }
            scope.launch {
                bgTaskManager.evaluateAndScheduleNext()
            }
        }
    }

    fun onBackgroundWakeup() {
        recoverStuckTasks()
    }

    private fun recoverStuckTasks() {
        scope.launch {
            repository.resetState(
                from = State.Running,
                to = State.Enqueued
            )
        }
    }
}