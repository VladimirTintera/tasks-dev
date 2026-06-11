package eu.tintera.guard

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

abstract class PendingTokenProducer(
    private val scope: CoroutineScope
) : TokenProducer, PendingTokenObservable {

    private val _pendingToken = MutableStateFlow<Set<Token>>(emptySet())
    override val pendingToken: Flow<Set<Token>> = _pendingToken.asStateFlow()

    private val channel = Channel<Token>(Channel.UNLIMITED)

    protected fun produce(token: Token) {

        channel.trySend(token)

        _pendingToken.update { it + token }
        scope.launch {
            token.state.first { it.isFinal }
            _pendingToken.update { it - token }
        }
    }

    override fun token() = channel.receiveAsFlow()
}