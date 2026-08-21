package eu.tintera.background.guard

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * A [TokenProducer] for sources that hand out tokens on their own schedule — the system notifies,
 * a token appears, and it stays valid only until its own deadline expires.
 *
 * The set of pending tokens is the single source of truth. There is deliberately no separate buffer:
 * a token that nobody consumes before it expires has to disappear, and a queue cannot forget. An
 * expired token handed to a consumer looks alive for a moment and then immediately reports itself as
 * finished, which is indistinguishable from "everything that covered us just died".
 *
 * Because the pending set is a [StateFlow], a consumer that starts collecting later still sees every
 * token that is currently alive — buffering is preserved, staleness is not.
 */
abstract class PendingTokenProducer(
    private val scope: CoroutineScope
) : TokenProducer, PendingTokenObservable {

    private val _pendingToken = MutableStateFlow<Set<Token>>(emptySet())
    override val pendingToken: Flow<Set<Token>> = _pendingToken.asStateFlow()

    protected fun produce(token: Token) {
        if (token.state.value.isFinal) return

        _pendingToken.update { it + token }
        scope.launch {
            token.state.first { it.isFinal }
            _pendingToken.update { it - token }
        }
    }

    override fun token(): Flow<Token> = _pendingToken.asEventStream()
}
