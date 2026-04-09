package eu.tintera.guard

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import platform.UIKit.UIApplication
import platform.UIKit.UIBackgroundTaskInvalid


internal class IosBackgroundTaskToken(
    isBackground: StateFlow<Boolean>,
    scope: CoroutineScope,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
    expirationHandler: () -> Unit
) : AppStateAwareToken<ULong>(isBackground, scope, dispatcher, expirationHandler) {

    override fun acquireSystemResource(): Lock<ULong>? {
        val taskId = UIApplication.sharedApplication.beginBackgroundTaskWithExpirationHandler {
            expirationHandler()
        }

        return if (taskId != UIBackgroundTaskInvalid) {
            Lock(taskId, taskId.toString())
        } else {
            null
        }
    }

    override fun releaseSystemResource(lock: ULong) {
        UIApplication.sharedApplication.endBackgroundTask(lock)
    }
}