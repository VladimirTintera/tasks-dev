package eu.tintera.background.guard

import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Represents a context in which a task is executed under a specific lock or system constraint.
 */
interface ExecutionContext {
    /**
     * Releases the underlying lock or resource associated with this context.
     */
    suspend fun release()

    /**
     * A flow representing whether the execution context has expired (e.g., due to a system timeout).
     */
    val isExpired: StateFlow<Boolean>
}

/**
 * Executes the given [block] within this [ExecutionContext], ensuring that the context
 * is released after completion.
 *
 * If [ExecutionContext.isExpired] becomes true during execution, the scope is cancelled
 * to prevent further processing.
 */
suspend inline fun ExecutionContext.use(
    crossinline block: suspend ExecutionContext.() -> Unit
) = coroutineScope {

    val expirationJob = launch {
        isExpired.first { it } // Čekáme na signál, že nám došel čas

        // Zrušíme TENTO specifický coroutineScope.
        // Všechny child coroutiny (včetně blocku) dostanou CancellationException.
        this@coroutineScope.cancel("Context expired by system limit")
    }

    try {
        // 2. Vykonáme samotnou byznys logiku (ExecutionContext je receiver)
        block()
    } finally {
        // Pokud block() doběhl úspěšně dřív, než vypršel čas, musíme hlídače zrušit.
        expirationJob.cancel()

        // VŽDY bezpečně uvolníme systémový zámek
        release()
    }
}