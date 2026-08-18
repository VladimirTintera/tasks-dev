package eu.tintera.background.guard.fakes

import eu.tintera.background.guard.AbstractToken

// Fake token that only records what happened to it.
class FakeToken(override val tag: String = "Fake") : AbstractToken() {
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