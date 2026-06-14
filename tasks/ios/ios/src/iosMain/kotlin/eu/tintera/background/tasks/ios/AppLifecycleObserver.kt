package eu.tintera.background.tasks.ios

import eu.tintera.background.tasks.core.AppStateObserver
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

class AppLifecycleObserver : AppStateObserver {
    private val _isBackground = MutableStateFlow(
        UIApplication.sharedApplication.applicationState == UIApplicationState.UIApplicationStateBackground
    )

    override val isBackground: StateFlow<Boolean> = _isBackground.asStateFlow()

    init {
        setupLifecycleObservers()
    }

    private fun setupLifecycleObservers() {
        val center = NSNotificationCenter.defaultCenter

        center.addObserverForName(
            name = UIApplicationWillEnterForegroundNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue
        ) { _ ->
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
}