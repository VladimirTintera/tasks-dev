package eu.tintera.guard.fakes

import eu.tintera.guard.AbstractToken

// Falešný token, který si jen pamatuje, co se s ním stalo
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