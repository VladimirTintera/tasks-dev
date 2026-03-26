package eu.tintera.tasks.core.fakes

import eu.tintera.tasks.core.ApplicationScope
import eu.tintera.tasks.core.dispatchers
import eu.tintera.tasks.core.locks.ReactiveCompositeTokenProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ReactiveCompositeTokenProviderTest {

    @Test
    fun `when only one token expires, global expiration is triggered`() = runTest {
        val producer = FakeTokenProducer()
        val provider = ReactiveCompositeTokenProvider(
            scope = ApplicationScope(SupervisorJob()),
            dispatcher = dispatchers().default,
            producers = listOf(producer)
        )

        var globalExpired = false
        provider.acquire { globalExpired = true }

        // Vyemitujeme token
        producer.emitToken(FakeToken())
        assertFalse(globalExpired, "Global expiration should not happen yet")

        // Simulujeme, že token vypršel
        producer.simulateExpiration()

        // Jelikož to byl jediný token, orchestrátor musí odpálit globální smrt
        assertTrue(globalExpired, "Global expiration should be triggered when the last token dies")
    }


    @Test
    fun `when multiple tokens exist, global expiration waits for the last one`() = runTest {
        val producerA = FakeTokenProducer()
        val producerB = FakeTokenProducer()
        val provider = ReactiveCompositeTokenProvider(
            scope = ApplicationScope(SupervisorJob()),
            dispatcher = dispatchers().default,
            producers = listOf(producerA, producerB)
        )

        var globalExpired = false
        provider.acquire { globalExpired = true }

        // Oba producenti dodají své štíty (máme dvojitý štít)
        producerA.emitToken(FakeToken("A"))
        runCurrent()
        producerB.emitToken(FakeToken("B"))
        runCurrent()

        // Umře štít A (např. 30s background task)
        producerA.simulateExpiration()
        runCurrent()

        // TADY JE TA MAGIE: Orchestrátor ví, že B ještě žije!
        assertFalse(globalExpired, "Global expiration should NOT trigger, Token B is still active")

        // Teprve když umře i štít B, orchestrátor to zabalí
        producerB.simulateExpiration()
        runCurrent()
        assertTrue(globalExpired, "Global expiration MUST trigger when the final token dies")
    }

    @Test
    fun `when producer emits a new token, the old one is canceled immediately`() = runTest {
        val producer = FakeTokenProducer()
        val provider = ReactiveCompositeTokenProvider(
            scope = ApplicationScope(SupervisorJob()),
            dispatcher = dispatchers().default,
            producers = listOf(producer)
        )

        provider.acquire {}

        runCurrent()

        val oldToken = FakeToken("Old")
        val newToken = FakeToken("New")

        // Producent pošle první token
        producer.emitToken(oldToken)
        runCurrent()
        assertFalse(oldToken.isCanceled, "Old token should be active")

        // Producent zničehonic pošle NOVÝ token
        producer.emitToken(newToken)
        runCurrent()

        // Orchestrátor ho musí okamžitě zaříznout synchronní metodou cancel()
        assertTrue(oldToken.isCanceled, "Old token must be canceled when replaced")
        assertFalse(oldToken.isReleased, "Old token should NOT be released (avoids DB IO)")
        assertFalse(newToken.isCanceled, "New token should remain active")
    }

    @Test
    fun `when composite token is released, all active sub-tokens are released`() = runTest {

        val producerA = FakeTokenProducer()
        val producerB = FakeTokenProducer()
        val provider = ReactiveCompositeTokenProvider(
            scope = this,
            dispatcher = dispatchers().default,
            producers = listOf(producerA, producerB)
        )

        // Získáme ten náš orchestrální CompositeToken
        val compositeToken = provider.acquire {}
        runCurrent()

        val tokenA = FakeToken("A")
        val tokenB = FakeToken("B")
        producerA.emitToken(tokenA)
        runCurrent()
        producerB.emitToken(tokenB)
        runCurrent()

        // Byznys logika skončila, voláme release()
        compositeToken.release()
        runCurrent()

        // Všechny držené tokeny musí dostat povel k release
        assertTrue(tokenA.isReleased, "Token A should be released")
        assertTrue(tokenB.isReleased, "Token B should be released")
        assertFalse(tokenA.isCanceled, "Token A should not be canceled")
        assertFalse(tokenB.isCanceled, "Token B should not be canceled")
    }
}