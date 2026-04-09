package eu.tintera.guard

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class AppStateAwareTokenProducer(
    private val tokenFactory: (expirationHandler: () -> Unit) -> Token
) : TokenProducer {
    override fun token(onExpire: () -> Unit): Flow<Token> = flow {
        emit(tokenFactory(onExpire))
    }
}