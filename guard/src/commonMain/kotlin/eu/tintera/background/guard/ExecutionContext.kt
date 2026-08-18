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
        isExpired.first { it } // wait for the signal that our time is up

        // Cancel THIS scope; every child coroutine (the block included) gets a CancellationException.
        this@coroutineScope.cancel("Context expired by system limit")
    }

    try {
        // 2. Run the actual work (ExecutionContext is the receiver).
        block()
    } finally {
        // If block() finished before the time ran out, cancel the watchdog.
        expirationJob.cancel()

        // ALWAYS release the system lock.
        release()
    }
}