package eu.tintera.guard

import eu.tintera.guard.fakes.FakeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class TestExhaustibleTokenProducer : ExhaustibleTokenProducer("Test") {
    // Místo obyčejného Int použijeme StateFlow, abychom na změnu mohli čekat
    private val _produceCallCount = MutableStateFlow(0)
    val produceCallCountFlow = _produceCallCount.asStateFlow()

    // Pro zpětnou kompatibilitu s jinými testy
    val produceCallCount: Int get() = _produceCallCount.value

    lateinit var capturedExpireCallback: () -> Unit

    override suspend fun produce(): Token {
        val token = FakeToken()
        capturedExpireCallback = { token.cancel() }
        _produceCallCount.update { it + 1 } // Atomický update
        return token
    }
}