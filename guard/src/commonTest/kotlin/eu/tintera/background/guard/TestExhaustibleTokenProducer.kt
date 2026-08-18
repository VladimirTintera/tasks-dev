package eu.tintera.background.guard

import eu.tintera.background.guard.fakes.FakeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class TestExhaustibleTokenProducer : ExhaustibleTokenProducer("Test") {
    // A StateFlow rather than a plain Int, so tests can await the change.
    private val _produceCallCount = MutableStateFlow(0)
    val produceCallCountFlow = _produceCallCount.asStateFlow()

    // Kept for other tests.
    val produceCallCount: Int get() = _produceCallCount.value

    lateinit var capturedExpireCallback: () -> Unit

    override suspend fun produce(): Token {
        val token = FakeToken()
        capturedExpireCallback = { token.cancel() }
        _produceCallCount.update { it + 1 } // atomic update
        return token
    }
}