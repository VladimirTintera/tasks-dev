package eu.tintera.background.tasks.ios

import eu.tintera.background.guard.AbstractToken
import platform.BackgroundTasks.BGTask

internal class BgTaskToken(
    identifier: String,
    private val task: BGTask
) : AbstractToken() {

    override val tag = "BgTask:$identifier"

    init {
        task.expirationHandler = {
            finishWithCancel()
        }
    }

    override suspend fun onRelease() {
        task.setTaskCompletedWithSuccess(true)
    }

    override fun onCancel() {
        task.setTaskCompletedWithSuccess(false)
    }

    fun cancel() {
        finishWithCancel()
    }
}