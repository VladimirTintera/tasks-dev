package eu.tintera.background.tasks.core.fakes

import eu.tintera.background.guard.Token
import eu.tintera.background.guard.TokenProducer
import eu.tintera.background.guard.fakes.FakeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

// Fake producer whose tokens can be emitted or expired on demand.
class FakeTokenProducer : TokenProducer {
    private val tokenFlow = MutableSharedFlow<FakeToken>()
    private var expireCallback: (() -> Unit)? = null

    override fun token(): Flow<Token> {
        return tokenFlow
    }

    // Test helper: push a new token to the orchestrator.
    suspend fun emitToken(token: FakeToken) {
        expireCallback = {
            token.cancel()
        }
        tokenFlow.emit(token)
    }

    suspend fun emitNewToken(): FakeToken {
        val token = FakeToken()
        tokenFlow.emit(token)
        expireCallback = {
            token.cancel()
        }
        return token
    }

    // Test helper: simulate the iOS watchdog firing.
    fun simulateExpiration() {
        expireCallback?.invoke()
    }
}