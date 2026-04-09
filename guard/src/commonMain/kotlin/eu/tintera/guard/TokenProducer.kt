package eu.tintera.guard

import kotlinx.coroutines.flow.Flow

interface TokenProducer {
    fun token(onExpire: () -> Unit): Flow<Token>
}