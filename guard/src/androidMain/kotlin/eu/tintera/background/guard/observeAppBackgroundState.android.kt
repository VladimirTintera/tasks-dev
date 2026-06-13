package eu.tintera.background.guard

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal enum class SwitchableState {
    FOREGROUND,
    BACKGROUND
}

@PublishedApi
internal fun observeAppBackgroundState(
    scope: CoroutineScope
) = callbackFlow {
    // Vytvoříme observer, který naslouchá celé aplikaci
    val observer = object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            // Aplikace je viditelná (Foreground)
            trySend(SwitchableState.FOREGROUND)
        }

        override fun onStop(owner: LifecycleOwner) {
            // Aplikace není viditelná a běží na pozadí (Background)
            trySend(SwitchableState.BACKGROUND)
        }
    }

    // ProcessLifecycleOwner se MUSÍ registrovat z Main threadu
    withContext(Dispatchers.Main.immediate) {
        val lifecycle = ProcessLifecycleOwner.get().lifecycle
        lifecycle.addObserver(observer)

        // Okamžitě si zjistíme a pošleme aktuální stav
        val isBackground = !lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        trySend(if (isBackground) SwitchableState.BACKGROUND else SwitchableState.FOREGROUND)
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
    initialValue = SwitchableState.FOREGROUND // Bezpečný default, hned vzápětí ho withContext přepíše reálnou hodnotou
)