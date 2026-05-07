package eu.tintera.tasks

import co.touchlab.kermit.Logger
import eu.tintera.guard.Token
import eu.tintera.guard.TokenObservable
import eu.tintera.guard.TokenState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class TokenObserver(
    private val scope: ApplicationScope,
    private val tokenObservable: TokenObservable,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {

    private val logger = Logger.withTag("TokenObserver")

    data class TokenWithState(
        val token: Token,
        val state: TokenState
    )

    fun start() {
        scope.launch(dispatcher) {
            tokenObservable.acquiredTokens.buffer(
                capacity = 100,
                onBufferOverflow = BufferOverflow.DROP_OLDEST
            ).map { token ->
                token.state.map { state ->
                    TokenWithState(
                        token = token,
                        state = state
                    )
                }
            }.flattenMerge(concurrency = Int.MAX_VALUE).collect { token ->
                logger.i { "Token '${token.token.tag}' is ${token.state}" }
            }
        }
    }
}