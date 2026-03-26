package eu.tintera.tasks.core.fakes

import eu.tintera.tasks.core.locks.Token
import eu.tintera.tasks.core.locks.TokenProducer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

// Falešný producent, u kterého můžeme "na povel" emitovat tokeny nebo je expirovat
class FakeTokenProducer : TokenProducer {
    private val tokenFlow = MutableSharedFlow<Token>()
    private var expireCallback: (() -> Unit)? = null

    override fun token(onExpire: () -> Unit): Flow<Token> {
        this.expireCallback = onExpire
        return tokenFlow
    }

    // Pomocná metoda pro testy: Pošle nový token do orchestrátoru
    suspend fun emitToken(token: Token) {
        tokenFlow.emit(token)
    }

    // Pomocná metoda pro testy: Simuluje, že iOS odpálil Watchdoga
    fun simulateExpiration() {
        expireCallback?.invoke()
    }
}