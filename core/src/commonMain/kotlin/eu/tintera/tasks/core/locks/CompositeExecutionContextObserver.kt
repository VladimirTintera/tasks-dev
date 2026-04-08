package eu.tintera.tasks.core.locks

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

class CompositeExecutionContextObserver(
    observers: List<ExecutionContextObserver> = emptyList()
) : ExecutionContextObserver, ExecutionContextObserverRegistry {
    private val _observers = MutableStateFlow(observers)

    override fun registerObserver(observer: ExecutionContextObserver) {
        _observers.update { it + observer }
    }

    override fun onStarted() {
        _observers.value.forEach {
            try {
                it.onStarted()
            } catch (_: Throwable) {
            }
        }
    }

    override suspend fun onPreRelease() = coroutineScope {
        // Všechny spustíme paralelně
        _observers.value.forEach { observer ->
            launch {
                try {
                    observer.onPreRelease()
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Throwable) {
                    // Ignorujeme chyby jednotlivých observerů
                }
            }
        }
    }

    override fun onPreCancel() {
        _observers.value.forEach {
            try {
                it.onPreCancel()
            } catch (_: Throwable) {
            }
        }
    }
}