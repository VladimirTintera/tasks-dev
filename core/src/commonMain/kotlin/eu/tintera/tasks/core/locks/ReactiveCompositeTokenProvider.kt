package eu.tintera.tasks.core.locks

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class ReactiveCompositeTokenProvider(
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val producers: List<TokenProducer>
) : TokenProvider {

    override suspend fun acquire(
        expirationHandler: () -> Unit
    ): Token {

        val activeTokens = MutableStateFlow<Map<TokenProducer, Token>>(emptyMap())

        // Sjednotíme všechny producery do jednoho streamu
        val mergedFlow = producers.map { producer ->
            producer.token {
                activeTokens.updateAndGet { current ->
                    current - producer
                }.also {
                    if (it.isEmpty()) expirationHandler()
                }
            }.onEach {
                println("token emmited")
            }.map {
                producer to it
            }
        }.merge()

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
    private val activeTokens: StateFlow<Map<TokenProducer, Token>>,
    private val collectionJob: Job // Potřebujeme zastavit sbírání eventů!
) : Token {

    override suspend fun release() {
        collectionJob.cancel() // Zastaví channelFlow v producerech = vše se uklidí
        activeTokens.value.forEach { it.value.release() }
    }

    override fun cancel() {
        collectionJob.cancel()
        activeTokens.value.forEach { it.value.cancel() }
    }
}