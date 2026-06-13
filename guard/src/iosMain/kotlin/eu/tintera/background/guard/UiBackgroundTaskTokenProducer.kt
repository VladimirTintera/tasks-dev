package eu.tintera.background.guard

import platform.UIKit.UIApplication
import platform.UIKit.UIBackgroundTaskIdentifier
import platform.UIKit.UIBackgroundTaskInvalid

internal class UiBackgroundTaskTokenProducer : ExhaustibleTokenProducer("UiBackgroundTask") {
    override suspend fun produce(): Token? = UiBackgroundTaskToken().takeIf { it.isValid() }
}

internal class UiBackgroundTaskToken : AbstractToken() {

    val taskId: UIBackgroundTaskIdentifier = UIApplication.sharedApplication.beginBackgroundTaskWithExpirationHandler {
        finishWithCancel()
    }

    override val tag = "UiBackgroundTask:$taskId"

    override suspend fun onRelease() {
        endBackgroundTask()
    }

    override fun onCancel() {
        endBackgroundTask()
    }

    private fun endBackgroundTask() {
        if (isValid()) UIApplication.sharedApplication.endBackgroundTask(taskId)
    }

    fun isValid() = taskId != UIBackgroundTaskInvalid
}