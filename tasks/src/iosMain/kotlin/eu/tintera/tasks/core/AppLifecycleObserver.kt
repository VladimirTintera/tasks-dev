package eu.tintera.tasks.core

import eu.tintera.tasks.State
import eu.tintera.tasks.core.data.Repository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationDidEnterBackgroundNotification
import platform.UIKit.UIApplicationState
import platform.UIKit.UIApplicationWillEnterForegroundNotification

class AppLifecycleObserver(
    private val scope: ApplicationScope,
    private val dispatchers: AppDispatchers,
    private val repository: Repository
) {
    private val _isBackground = MutableStateFlow(
        UIApplication.sharedApplication.applicationState == UIApplicationState.UIApplicationStateBackground
    )

    val isBackground: StateFlow<Boolean> = _isBackground.asStateFlow()

    init {
        recoverStuckTasks()
        setupLifecycleObservers()
    }

    private fun setupLifecycleObservers() {
        val center = NSNotificationCenter.defaultCenter

        // 2. Úklid při návratu ze zmražení do popředí (Foreground)
        center.addObserverForName(
            name = UIApplicationWillEnterForegroundNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue
        ) { _ ->
            recoverStuckTasks()
            _isBackground.update { false }
        }

        center.addObserverForName(
            name = UIApplicationDidEnterBackgroundNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue
        ) { _ ->
            _isBackground.update { true }
        }
    }

    private fun recoverStuckTasks() {
        scope.launch(dispatchers.io) {
            repository.resetState(
                from = State.Running,
                to = State.Enqueued
            )
        }
    }
}