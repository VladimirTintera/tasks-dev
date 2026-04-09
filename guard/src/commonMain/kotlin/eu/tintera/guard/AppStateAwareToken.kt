package eu.tintera.guard

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.concurrent.atomics.AtomicReference


internal data class Lock<T : Any>(
    val lock: T,
    val id: String
)

internal sealed class TokenState<out T> {
    object Idle : TokenState<Nothing>() // Ekvivalent tvého UIBackgroundTaskInvalid (-1L)
    data class Acquired<T: Any>(val lock: Lock<T>) : TokenState<T>() // Ekvivalent aktivního ID (> 0)
    object Terminated : TokenState<Nothing>() // Ekvivalent tvého RELEASED_STATE (-2L)
}

internal abstract class AppStateAwareToken<T: Any>(
    private val isBackground: StateFlow<Boolean>,
    scope: CoroutineScope,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
    protected val expirationHandler: () -> Unit
) : Token {

    // VŠE je nyní v jedné atomické referenci!
    private val state = AtomicReference<TokenState<T>>(TokenState.Idle)

    private val job = scope.launch(dispatcher) {
        isBackground.collect { background ->
            if (background) {
                tryAcquire()
            } else {
                pauseToForeground()
            }
        }
    }

    private fun tryAcquire() {
        // Fast-fail: Pokud už jsme mrtví nebo něco držíme, ani nebudíme systém
        val initial = state.load()
        if (initial === TokenState.Terminated || initial is TokenState.Acquired) return

        // Jdeme žádat systém (Může trvat!)
        val newLock = acquireSystemResource() ?: return

        // Nyní musíme výsledek atomicky zapsat, ALE situace se mohla změnit
        while (true) {
            val current = state.load()

            // Pokud nám do toho mezitím vlezl cancel() nebo návrat do foregroundu,
            // musíme nově nabytý zámek okamžitě zahodit a nepokračovat.
            if (current === TokenState.Terminated || current is TokenState.Acquired) {
                releaseSystemResource(newLock.lock)
                EventBus.send(TAG, "Discarded late lock (state changed).")
                return
            }

            // Pokud jsme pořád Idle, zkusíme to přepsat
            if (state.compareAndSet(current, TokenState.Acquired(newLock))) {
                EventBus.send(TAG, "System resource acquired '${newLock.id}'")
                return
            }
        }
    }

    private fun pauseToForeground() {
        while (true) {
            val current = state.load()

            // Nemáme co pauzovat
            if (current === TokenState.Terminated || current === TokenState.Idle) return

            // Pokus o návrat do Idle
            if (state.compareAndSet(current, TokenState.Idle)) {
                if (current is TokenState.Acquired) {
                    releaseSystemResource(current.lock.lock)
                    EventBus.send(TAG, "System resource '${current.lock.id}' released (App in foreground).")
                }
                return
            }
        }
    }

    override suspend fun release() {
        finish("Releasing")
    }

    override fun cancel() {
        finish("Canceling")
    }

    private fun finish(message: String) {
        job.cancel()

        while (true) {
            val current = state.load()

            // Už jsme terminováni jiným vláknem
            if (current === TokenState.Terminated) return

            // Atomický posun do konečného stavu
            if (state.compareAndSet(current, TokenState.Terminated)) {
                if (current is TokenState.Acquired) {
                    releaseSystemResource(current.lock.lock)
                    EventBus.send(TAG, "$message. Token '${current.lock.id}' is successfully ended.")
                }
                EventBus.send(TAG, "$message. Token is Terminated.")
                break
            }
        }
    }

    protected abstract fun acquireSystemResource(): Lock<T>?
    protected abstract fun releaseSystemResource(lock: T)

    companion object {
        private const val TAG = "AppStateAwareToken"
    }
}