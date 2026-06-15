package eu.tintera.background.guard

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*

internal class CompositeTokenProducer(
    private val scope: CoroutineScope,
    producers: List<TokenProducer>,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val onTokenProducerRegistered: (TokenProducer) -> Unit
) : TokenProducer, TokenProducerRegistry, TokenObservable {

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

    override fun token(): Flow<Token> = flow {
        val activeTokens = MutableStateFlow<Set<Token>>(emptySet())
        var combinedTokenRef: CompositeToken? = null

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

        val collectionJob = scope.launch(dispatcher) collectionScope@{
            mergedFlow.collect { token ->
                activeTokens.update { it + token }
                _acquiredTokens.tryEmit(token)
                token.markAsActive()

                token.invokeOnPreCancel {
                    val allCancelled = activeTokens.value.all { it.state.value == TokenState.CANCELLED }
                    if (allCancelled) {
                        combinedTokenRef?.triggerCancel()
                    }
                }

                launch {
                    token.state.first { it.isFinal }
                    activeTokens.updateAndGet { it - token }.also {
                        if (it.isEmpty()) {
                            this@collectionScope.cancel()
                        }
                    }
                }
            }
        }

        var emitted = false
        try {
            activeTokens.first { it.isNotEmpty() }

            val combinedToken = CompositeToken(
                releaseAction = {
                    collectionJob.cancel()
                    coroutineScope {
                        activeTokens.getAndUpdate { emptySet() }.forEach {
                            launch { it.release() }
                        }
                    }
                },
                cancelAction = {
                    collectionJob.cancel()
                    scope.launch {
                        activeTokens.getAndUpdate { emptySet() }.forEach {
                            launch { it.release() }
                        }
                    }
                }
            )

            combinedTokenRef = combinedToken
            emitted = true
            emit(combinedToken)
        } finally {
            if (!emitted) {
                collectionJob.cancel()
            }
        }
    }
}


private class CompositeToken(
    private val releaseAction: suspend () -> Unit,
    private val cancelAction: () -> Unit
) : AbstractToken() {
    override val tag = "CompositeToken"

    override suspend fun onRelease() {
        releaseAction()
    }

    override fun onCancel() {
        cancelAction()
    }

    fun triggerCancel() {
        finishWithCancel()
    }
}
