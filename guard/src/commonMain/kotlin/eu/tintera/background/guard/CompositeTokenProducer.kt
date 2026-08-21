package eu.tintera.background.guard

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlin.concurrent.atomics.AtomicReference

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
        // Read from the collection job, written here — atomic rather than a plain var.
        val combinedTokenRef = AtomicReference<CompositeToken?>(null)

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

                // Kept next to the asynchronous check below on purpose: this one runs synchronously,
                // which is the only thing fast enough when the OS revokes a permission and gives us
                // a microscopic window to react in.
                token.invokeOnPreCancel {
                    val allCancelled = activeTokens.value.all { it.state.value == TokenState.CANCELLED }
                    if (allCancelled) {
                        combinedTokenRef.load()?.triggerCancel()
                    }
                }

                launch {
                    token.state.first { it.isFinal }
                    if (activeTokens.updateAndGet { it - token }.isEmpty()) {
                        // Nothing covers us any more, so the session has to end — but only if it
                        // ever started. Before the combined token is emitted an empty set merely
                        // means "no permission yet"; cancelling the collection here would mean we
                        // never hear about the next one and `acquire()` would hang forever.
                        combinedTokenRef.load()?.triggerCancel()
                    }
                }
            }
        }

        var emitted = false
        try {
            while (!emitted) {
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

                combinedTokenRef.store(combinedToken)

                // The last token may have finished between the wait above and the line below.
                // Emitting now would hand out a permission that covers nothing, so wait for the
                // next one instead.
                if (activeTokens.value.isEmpty()) {
                    combinedTokenRef.compareAndSet(combinedToken, null)
                    continue
                }

                emitted = true
                emit(combinedToken)
            }
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
