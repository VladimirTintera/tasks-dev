package eu.tintera.guard

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationDidEnterBackgroundNotification
import platform.UIKit.UIApplicationState
import platform.UIKit.UIApplicationWillEnterForegroundNotification

internal actual fun observeAppBackgroundState(
    scope: CoroutineScope
): StateFlow<Boolean> = callbackFlow {
    val center = NSNotificationCenter.defaultCenter

    val foreground = center.addObserverForName(
        name = UIApplicationWillEnterForegroundNotification,
        `object` = null,
        queue = NSOperationQueue.mainQueue
    ) { _ ->
        trySend(false)
    }

    val background = center.addObserverForName(
        name = UIApplicationDidEnterBackgroundNotification,
        `object` = null,
        queue = NSOperationQueue.mainQueue
    ) { _ ->
        trySend(true)
    }

    awaitClose {
        center.removeObserver(foreground)
        center.removeObserver(background)
    }
}.stateIn(
    scope = scope,
    started = SharingStarted.WhileSubscribed(),
    initialValue = UIApplication.sharedApplication.applicationState == UIApplicationState.UIApplicationStateBackground
)