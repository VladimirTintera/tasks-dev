package eu.tintera.background.guard

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSThread
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationDidEnterBackgroundNotification
import platform.UIKit.UIApplicationState
import platform.UIKit.UIApplicationWillEnterForegroundNotification
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_sync

internal enum class SwitchableState {
    FOREGROUND,
    BACKGROUND,
}

internal fun observeAppBackgroundState(
    scope: CoroutineScope
): StateFlow<SwitchableState> {

    fun backgroundState(): SwitchableState {
        val isBackground = if (NSThread.isMainThread) {
            UIApplication.sharedApplication.applicationState == UIApplicationState.UIApplicationStateBackground
        } else {
            var isBackground = false
            dispatch_sync(dispatch_get_main_queue()) {
                isBackground =
                    UIApplication.sharedApplication.applicationState == UIApplicationState.UIApplicationStateBackground
            }
            isBackground
        }

        return if (isBackground) SwitchableState.BACKGROUND else SwitchableState.FOREGROUND
    }


    return callbackFlow {
        val center = NSNotificationCenter.defaultCenter

        val foreground = center.addObserverForName(
            name = UIApplicationWillEnterForegroundNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue
        ) { _ ->
            trySend(SwitchableState.FOREGROUND)
        }

        val background = center.addObserverForName(
            name = UIApplicationDidEnterBackgroundNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue
        ) { _ ->
            trySend(SwitchableState.BACKGROUND)
        }


        awaitClose {
            center.removeObserver(foreground)
            center.removeObserver(background)
        }
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = backgroundState()
    )
}