package eu.tintera.tasks.core.fakes

import eu.tintera.tasks.core.locks.Token

// Falešný token, který si jen pamatuje, co se s ním stalo
class FakeToken(val name: String = "Fake") : Token {
    var isReleased = false
        private set
    var isCanceled = false
        private set

    override suspend fun release() {
        isReleased = true
    }

    override fun cancel() {
        isCanceled = true
    }
}

