package eu.tintera.guard

import eu.tintera.guard.EventBus
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class CompositeTokenProvider(
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    producers: List<TokenProducer>
) : TokenProvider, TokenRegistry {
    private val registeredProducers = MutableStateFlow(producers.toSet())
    override fun registerProducer(producer: TokenProducer) {
        registeredProducers.update { it + producer }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun acquire(
        expirationHandler: () -> Unit
    ): Token {

        val activeTokens = MutableStateFlow<Map<TokenProducer, Token>>(emptyMap())

        val continuousProducersStream = flow {
            val seen = mutableSetOf<TokenProducer>()
            registeredProducers.collect { currentSet ->
                currentSet.forEach { producer ->
                    // seen.add() vrátí true jen tehdy, když tam prvek ještě nebyl.
                    // Tím pádem propustíme dál vždy jen ty úplně nové!
                    if (seen.add(producer)) {
                        emit(producer)
                    }
                }
            }
        }

        val mergedFlow = continuousProducersStream.flatMapMerge(concurrency = Int.MAX_VALUE) { producer ->
            producer.token {
                EventBus.send("ReactiveCompositeTokenProvider", "token expired: $producer")
                var token: Token? = null
                activeTokens.updateAndGet { current ->
                    token = current[producer]
                    current - producer
                }.also {
                    token?.cancel()
                    if (it.isEmpty()) expirationHandler()
                }
            }.map {
                producer to it
            }
        }

        // Spustíme asynchronní sběr (job se ukončí, až se zavolá release() na tokenu)
        val collectionJob = scope.launch(dispatcher) {
            mergedFlow.collect { (producer, newToken) ->
                // Pomocná proměnná pro zachycení starého tokenu,
                // který případně musíme zrušit.
                var tokenToCancel: Token? = null

                activeTokens.update { currentMap ->
                    val oldToken = currentMap[producer]

                    // Zachytíme starý token (pokud ho přepisujeme něčím novým)
                    if (oldToken != null && oldToken !== newToken) {
                        tokenToCancel = oldToken
                    } else {
                        // Musíme vynulovat pro případ, že se tento blok opakuje kvůli CAS retry!
                        tokenToCancel = null
                    }

                    // Vrátíme novou mapu, ta se atomicky uloží
                    currentMap + (producer to newToken)
                }

                // JSME VENKU Z ATOMICKÉHO BLOKU (Bezpečný Side-Effect)
                // Pokud jsme někoho nahradili, teď ho bezpečně a synchronně zrušíme.
                tokenToCancel?.cancel()
            }
        }
        return CompositeToken(activeTokens, collectionJob)
    }
}

private class CompositeToken(
    private val activeTokens: MutableStateFlow<Map<TokenProducer, Token>>,
    private val collectionJob: Job // Potřebujeme zastavit sbírání eventů!
) : Token {

    override suspend fun release() {
        collectionJob.cancel() // Zastaví channelFlow v producerech = vše se uklidí
        activeTokens.getAndUpdate { emptyMap() }.forEach { it.value.release() }
    }

    override fun cancel() {
        EventBus.send("CompositeToken", "cancel called. ActiveTokens: ${activeTokens.value.size}")
        collectionJob.cancel()
        activeTokens.getAndUpdate { emptyMap() }.forEach { it.value.cancel() }
    }
}