package eu.tintera.background.tasks.core.fakes

import eu.tintera.background.guard.AbstractToken

class FakeToken(val name: String = "Fake") : AbstractToken() {
    override val tag = name

    var isReleased = false
        private set
    var isCanceled = false
        private set

    override suspend fun onRelease() {
        isReleased = true
    }

    override fun onCancel() {
        isCanceled = true
    }

    fun cancel() {
        finishWithCancel()
    }
}
