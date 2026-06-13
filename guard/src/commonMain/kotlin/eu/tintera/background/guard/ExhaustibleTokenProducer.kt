package eu.tintera.background.guard

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.mapNotNull

abstract class ExhaustibleTokenProducer(
    override val name: String
) : TokenProducer, ExecutionContextObserver, Exhaustible {

    private val _isExhausted = MutableStateFlow(false)
    override val isExhausted = _isExhausted.asStateFlow()

    override val providedObservers: List<ExecutionContextObserver>
        get() = listOf(this)

    abstract suspend fun produce(): Token?

    override fun token(): Flow<Token> = isExhausted.mapNotNull { exhausted ->
        if (!exhausted) {
            val token = produce()
            token?.invokeOnPreCancel {
                _isExhausted.value = true
            }
            token
        } else {
            null
        }
    }

    override fun onStarted() {
        _isExhausted.value = false
    }
}