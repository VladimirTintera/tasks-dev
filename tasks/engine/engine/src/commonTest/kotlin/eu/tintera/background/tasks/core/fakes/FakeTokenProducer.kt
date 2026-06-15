package eu.tintera.background.tasks.core.fakes

import eu.tintera.background.guard.Token
import eu.tintera.background.guard.TokenProducer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

// Falešný producent, u kterého můžeme "na povel" emitovat tokeny nebo je expirovat
class FakeTokenProducer : TokenProducer {
    private val tokenFlow = MutableSharedFlow<Token>()

    override fun token(): Flow<Token> {
        return tokenFlow
    }

    // Pomocná metoda pro testy: Pošle nový token do orchestrátoru
    suspend fun emitToken(token: Token) {
        tokenFlow.emit(token)
    }

    suspend fun emitNewToken(): FakeToken {
        val token = FakeToken()
        tokenFlow.emit(token)
        return token
    }
}