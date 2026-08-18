package eu.tintera.background.guard

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.getAndUpdate


/**
 * Represents a platform-specific system resource (e.g., UIBackgroundTaskIdentifier on iOS
 * or PowerManager.WakeLock on Android) held by the guard multiplexer.
 *
 * Platform implementations must ensure that cancellation events (when the OS revokes permissions)
 * propagate synchronously and instantly to stop pending tasks before the process is suspended or killed.
 */
interface Token {
    val tag: String

    val state: StateFlow<TokenState>

    /**
     * Registers a callback to be executed synchronously immediately before the token is cancelled.
     *
     * IMPORTANT: This callback is intentionally NOT a suspend function and must run completely
     * on the calling thread without dispatching to other threads. Operating systems (especially iOS)
     * grant a very limited execution window upon background task expiration and require synchronous,
     * immediate completion signaling. Any asynchronous thread switching inside the callback
     * would risk the process being suspended or terminated by the OS before the completion signal
     * can be sent.
     *
     * Implementations of the registered block must be extremely fast and lightweight.
     */
    fun invokeOnPreCancel(block: () -> Unit): DisposableHandle = DisposableHandle {}

    /**
     * Voluntarily releases the token after execution finishes.
     */
    suspend fun release()

    fun markAsActive()
}


abstract class AbstractToken : Token {
    private val _state = MutableStateFlow(TokenState.INITIATED)
    override val state: StateFlow<TokenState> = _state.asStateFlow()
    private val preCancelHooks = MutableStateFlow<List<() -> Unit>>(emptyList())

    override fun invokeOnPreCancel(block: () -> Unit): DisposableHandle {
        var wasAdded = false
        preCancelHooks.update { list ->
            if (_state.value == TokenState.CANCELLED || _state.value == TokenState.RELEASED) {
                return@update list // already finished — nothing to register
            }
            wasAdded = true
            list + block
        }

        if (!wasAdded) {
            if (_state.value == TokenState.CANCELLED) {
                try {
                    block()
                } catch (e: Exception) { /* Log error */
                }
            }
            return DisposableHandle {}
        }

        return DisposableHandle {
            preCancelHooks.update { it - block }
        }
    }

    /**
     * Called when the Guard voluntarily finishes execution and releases resources.
     */
    protected abstract suspend fun onRelease()

    /**
     * Called when the token is forced to terminate by the system (e.g., due to timeout or OS revocation).
     *
     * IMPORTANT: Implementations of this method must execute synchronously on the current thread
     * and complete immediately to prevent the OS from suspending the application before cleanup is finished.
     */
    protected abstract fun onCancel()

    override suspend fun release() {
        if (finishTo(TokenState.RELEASED)) onRelease()
    }

    protected fun finishWithCancel() {
        if (finishTo(TokenState.CANCELLED)) {
            // Take and clear the hooks atomically so they cannot run twice.
            val hooks = preCancelHooks.getAndUpdate { emptyList() }
            hooks.forEach { hook ->
                try {
                    hook()
                } catch (e: Exception) { /* Log error */
                }
            }
            onCancel()
        }
    }



    override fun markAsActive() {
        _state.compareAndSet(expect = TokenState.INITIATED, update = TokenState.ACTIVE)
    }

    private fun finishTo(
        state: TokenState
    ): Boolean = _state.compareAndSet(expect = TokenState.INITIATED, update = state)
            || _state.compareAndSet(expect = TokenState.ACTIVE, update = state)

}

enum class TokenState {
    INITIATED,
    ACTIVE,
    CANCELLED,
    RELEASED
}

val TokenState.isFinal: Boolean get() = this == TokenState.CANCELLED || this == TokenState.RELEASED

suspend fun <T> Token.use(
    block: suspend () -> T
): T = try {
    return block()
} finally {
    release()
}