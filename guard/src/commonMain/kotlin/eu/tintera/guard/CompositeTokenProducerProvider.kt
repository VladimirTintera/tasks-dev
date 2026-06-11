package eu.tintera.guard

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*

internal class CompositeTokenProducerProvider(
    private val scope: CoroutineScope,
    producers: List<TokenProducer>,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val onTokenProducerRegistered: (TokenProducer) -> Unit
) : TokenProvider, TokenProducerRegistry, TokenObservable {

    private val _acquiredTokens = MutableSharedFlow<Token>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val acquiredTokens = _acquiredTokens.asSharedFlow()

    private val registeredProducers = MutableStateFlow(producers.toSet())
    override fun registerProducer(
        producer: TokenProducer
    ) {
        registeredProducers.update { it + producer }
        onTokenProducerRegistered(producer)
    }

    override suspend fun acquire(
        onPreCancel: () -> Unit,
        onCancel: () -> Unit
    ): Token {

        val activeTokens = MutableStateFlow<Set<Token>>(emptySet())

        val mergedFlow = flow {
            val seen = mutableSetOf<TokenProducer>()
            registeredProducers.collect { currentSet ->
                currentSet.forEach { producer ->
                    if (seen.add(producer)) emit(producer)
                }
            }
        }.flatMapMerge(
            concurrency = Int.MAX_VALUE
        ) { it.token() }

        scope.launch(dispatcher) collectionScope@{

            mergedFlow.collect { token ->

                activeTokens.update { it + token }

                _acquiredTokens.tryEmit(token)

                token.markAsActive()

                token.invokeOnPreCancel {
                    val current = activeTokens.value
                    if (current.size == 1 && current.contains(token)) {
                        onPreCancel()
                    }
                }

                launch {
                    token.state.first { it.isFinal }

                    activeTokens.updateAndGet { it - token }.also {
                        if (it.isEmpty()) {
                            onCancel()
                            this@collectionScope.cancel()
                        }
                    }
                }
            }
        }

        activeTokens.first { it.isNotEmpty() }

        return ExecutionContextLock {
            coroutineScope {
                activeTokens.getAndUpdate { emptySet() }.forEach {
                    launch { it.release() }
                }
            }
        }
    }

    companion object {
        private const val TAG = "CompositeTokenProducerProvider"
    }
}

private class ExecutionContextLock(
    private val releaseAction: suspend () -> Unit
) : AbstractToken() {

    override val tag = "CompositeToken"

    override suspend fun onRelease() {
        releaseAction()
    }

    override fun onCancel() {}
}