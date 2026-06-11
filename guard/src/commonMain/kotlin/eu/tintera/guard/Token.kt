package eu.tintera.guard

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

interface Token {
    val tag: String

    val state: StateFlow<TokenState>

    fun invokeOnPreCancel(block: () -> Unit): DisposableHandle = DisposableHandle {}

    suspend fun release()

    fun markAsActive()
}


abstract class AbstractToken : Token {
    private val _state = MutableStateFlow(TokenState.INITIATED)
    override val state: StateFlow<TokenState> = _state.asStateFlow()
    private val preCancelHooks = MutableStateFlow<List<() -> Unit>>(emptyList())

    override fun invokeOnPreCancel(block: () -> Unit): DisposableHandle {
        // Pokud už je token zrušený, okamžitě spustíme hook a nevracíme užitečný handle,
        // protože už není co disposovat.
        if (_state.value == TokenState.CANCELLED) {
            try {
                block()
            } catch (e: Exception) { /* Log error */
            }
            return DisposableHandle {}
        }

        // Pokud je už releasnutý, hook nedělá nic (úkol skončil úspěšně)
        if (_state.value == TokenState.RELEASED) {
            return DisposableHandle {}
        }

        // Jinak ho klasicky zaregistrujeme
        preCancelHooks.update { it + block }
        return DisposableHandle {
            preCancelHooks.update { it - block }
        }
    }

    /** Voláno, když Guard dobrovolně ukončí práci. */
    protected abstract suspend fun onRelease()

    /** Voláno, když token násilně umírá zásahem zespodu (timeout, OS). */
    protected abstract fun onCancel()

    override suspend fun release() {
        if (finishTo(TokenState.RELEASED)) onRelease()
    }

    protected fun finishWithCancel() {
        if (finishTo(TokenState.CANCELLED)) {
            preCancelHooks.value.forEach { hook ->
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