package eu.tintera.tasks.core.fakes

import eu.tintera.tasks.core.locks.Token
import eu.tintera.tasks.core.locks.TokenProducer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlin.test.assertTrue

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

