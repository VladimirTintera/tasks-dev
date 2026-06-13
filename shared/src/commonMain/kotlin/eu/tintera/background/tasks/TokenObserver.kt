package eu.tintera.background.tasks

import co.touchlab.kermit.Logger
import eu.tintera.background.guard.Token
import eu.tintera.background.guard.TokenObservable
import eu.tintera.background.guard.TokenState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class TokenObserver(
    scope: ApplicationScope,
    private val observable: TokenObservable,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {

    private val logger = Logger.withTag("TokenObserver")

    data class TokenWithState(
        val token: Token,
        val state: TokenState
    )

    init {
        scope.launch(dispatcher) {
            observable.acquiredTokens.buffer(
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