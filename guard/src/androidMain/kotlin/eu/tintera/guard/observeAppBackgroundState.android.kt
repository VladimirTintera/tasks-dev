package eu.tintera.guard

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal actual fun observeAppBackgroundState(
    scope: CoroutineScope
): StateFlow<Boolean> = callbackFlow {
    // Vytvoříme observer, který naslouchá celé aplikaci
    val observer = object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            // Aplikace je viditelná (Foreground)
            trySend(false)
        }

        override fun onStop(owner: LifecycleOwner) {
            // Aplikace není viditelná a běží na pozadí (Background)
            trySend(true)
        }
    }

    // ProcessLifecycleOwner se MUSÍ registrovat z Main threadu
    withContext(Dispatchers.Main.immediate) {
        val lifecycle = ProcessLifecycleOwner.get().lifecycle
        lifecycle.addObserver(observer)

        // Okamžitě si zjistíme a pošleme aktuální stav
        val isBackground = !lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        trySend(isBackground)
    }

    // Když Coroutina skončí, musíme po sobě uklidit (opět na Main threadu)
    awaitClose {
        // awaitClose nemůže přímo suspendovat do Main vlákna,
        // proto použijeme GlobalScope nebo předaný scope pro rychlý cleanup
        scope.launch(Dispatchers.Main.immediate) {
            ProcessLifecycleOwner.get().lifecycle.removeObserver(observer)
        }
    }
}.stateIn(
    scope = scope,
    started = SharingStarted.Eagerly,
    initialValue = false // Bezpečný default, hned vzápětí ho withContext přepíše reálnou hodnotou
)