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
    // An observer for the whole application process.
    val observer = object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            // The application is visible.
            trySend(SwitchableState.FOREGROUND)
        }

        override fun onStop(owner: LifecycleOwner) {
            // The application is not visible and runs in the background.
            trySend(SwitchableState.BACKGROUND)
        }
    }

    // ProcessLifecycleOwner MUST be observed from the main thread.
    withContext(Dispatchers.Main.immediate) {
        val lifecycle = ProcessLifecycleOwner.get().lifecycle
        lifecycle.addObserver(observer)

        // Emit the current state right away.
        val isBackground = !lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        trySend(if (isBackground) SwitchableState.BACKGROUND else SwitchableState.FOREGROUND)
    }

    // Clean up when the flow is closed — again on the main thread.
    awaitClose {
        // awaitClose cannot suspend onto the main thread, hence a separate scope for the cleanup.
        scope.launch(Dispatchers.Main.immediate) {
            ProcessLifecycleOwner.get().lifecycle.removeObserver(observer)
        }
    }
}.stateIn(
    scope = scope,
    started = SharingStarted.Eagerly,
    initialValue = SwitchableState.FOREGROUND // safe default; withContext replaces it with the real value immediately
)