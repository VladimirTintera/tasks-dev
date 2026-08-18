package eu.tintera.background.tasks.core.fakes

import eu.tintera.background.guard.Token
import eu.tintera.background.guard.TokenProducer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

// Fake producer whose tokens can be emitted or expired on demand.
class FakeTokenProducer : TokenProducer {
    private val tokenFlow = MutableSharedFlow<Token>()

    override fun token(): Flow<Token> {
        return tokenFlow
    }

    // Test helper: push a new token to the orchestrator.
    suspend fun emitToken(token: Token) {
        tokenFlow.emit(token)
    }

    suspend fun emitNewToken(): FakeToken {
        val token = FakeToken()
        tokenFlow.emit(token)
        return token
    }
}