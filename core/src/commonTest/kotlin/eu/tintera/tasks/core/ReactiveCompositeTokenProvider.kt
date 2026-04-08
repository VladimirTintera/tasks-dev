package eu.tintera.tasks.core

import eu.tintera.tasks.core.fakes.FakeToken
import eu.tintera.tasks.core.fakes.FakeTokenProducer
import eu.tintera.tasks.core.locks.CompositeTokenProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CompositeTokenProviderTest {

    @Test
    fun `when only one token expires, global expiration is triggered`() = runTest {
        val producer = FakeTokenProducer()
        val provider = CompositeTokenProvider(
            scope = ApplicationScope(SupervisorJob()),
            dispatcher = dispatchers().default,
            producers = listOf(producer)
        )

        var globalExpired = false
        provider.acquire { globalExpired = true }

        // Vyemitujeme token
        producer.emitToken(FakeToken())
        runCurrent()
        assertFalse(globalExpired, "Global expiration should not happen yet")

        // Simulujeme, že token vypršel
        producer.simulateExpiration()

        runCurrent()

        // Jelikož to byl jediný token, orchestrátor musí odpálit globální smrt
        assertTrue(globalExpired, "Global expiration should be triggered when the last token dies")
    }


    @Test
    fun `when multiple tokens exist, global expiration waits for the last one`() = runTest {
        val producerA = FakeTokenProducer()
        val producerB = FakeTokenProducer()
        val provider = CompositeTokenProvider(
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
        val provider = CompositeTokenProvider(
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
        val provider = CompositeTokenProvider(
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

    @Test
    fun `acquire zachyti tokeny z initial produceru`() = runTest {
        // Arrange
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val producer1 = FakeTokenProducer()

        val compositeProvider = CompositeTokenProvider(
            scope = backgroundScope, // Poskytuje coroutines-test pro background joby
            dispatcher = dispatcher,
            producers = listOf(producer1)
        )

        // Act
        val compositeToken = compositeProvider.acquire {}
        val fakeToken = producer1.emitNewToken()

        // Prvotní stav - token žije
        assertFalse(fakeToken.isReleased)

        // Zrušíme kompozitní token
        compositeToken.release()

        // Assert - uvolnění se musí propagovat do vnitřního tokenu
        assertTrue(fakeToken.isReleased)
    }

    @Test
    fun `dynamicky pridany producer po acquire je korektne sledovan`() = runTest {
        // Arrange
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val compositeProvider = CompositeTokenProvider(
            scope = backgroundScope,
            dispatcher = dispatcher,
            producers = emptyList() // Startujeme prázdní!
        )

        // Act
        val compositeToken = compositeProvider.acquire {}

        // Dynamická registrace nového producera za běhu
        val dynamicProducer = FakeTokenProducer()
        compositeProvider.registerProducer(dynamicProducer)

        // Emitujeme token z nováčka
        val fakeToken = dynamicProducer.emitNewToken()

        // Assert uvolnění
        compositeToken.release()
        assertTrue(fakeToken.isReleased, "Dynamicky pridaný token nebyl uvolnen")
    }

    @Test
    fun `emitace noveho tokenu ze stejneho produceru zrusi ten stary`() = runTest {
        // Arrange
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val producer = FakeTokenProducer()
        val compositeProvider = CompositeTokenProvider(
            scope = backgroundScope,
            dispatcher = dispatcher,
            producers = listOf(producer)
        )

        // Act
        compositeProvider.acquire {}

        // Producer pošle první token
        val oldToken = producer.emitNewToken()
        assertFalse(oldToken.isCanceled)

        // Producer pošle NOVÝ token (nahrazuje starý)
        val newToken = producer.emitNewToken()

        // Assert - starý musí být okamžitě zrušen (cancel), nový žije
        assertTrue(oldToken.isCanceled, "Stary token nebyl zrusen pri nahrade")
        assertFalse(oldToken.isReleased)
        assertFalse(newToken.isCanceled)
    }

    @Test
    fun `globalni cancel propaguje zruseni do vsech aktivnich tokenu`() = runTest {
        // Arrange
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val producer1 = FakeTokenProducer()
        val producer2 = FakeTokenProducer()
        val compositeProvider = CompositeTokenProvider(
            scope = backgroundScope,
            dispatcher = dispatcher,
            producers = listOf(producer1, producer2)
        )

        // Act
        val compositeToken = compositeProvider.acquire {}
        val token1 = producer1.emitNewToken()
        val token2 = producer2.emitNewToken()

        // Zavoláme natvrdo cancel na celém kompozitu
        compositeToken.cancel()

        // Assert
        assertTrue(token1.isCanceled)
        assertTrue(token2.isCanceled)
        assertFalse(token1.isReleased)
        assertFalse(token2.isReleased)
    }

    @Test
    fun `expirace vsech vnitrnich tokenu zavola globalni expiration handler`() = runTest {
        // Arrange
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val producer = FakeTokenProducer()
        val compositeProvider = CompositeTokenProvider(
            scope = backgroundScope,
            dispatcher = dispatcher,
            producers = listOf(producer)
        )

        var globalExpirationCalled = false
        compositeProvider.acquire {
            globalExpirationCalled = true
        }

        // Získáme token, aby nebyla mapa prázdná
        producer.emitNewToken()
        assertFalse(globalExpirationCalled)

        // Act - Simulujeme, že systém (iOS) ukončil tento úkol
        producer.simulateExpiration()

        // Assert - globální handler se musel zavolat, protože to byl poslední (a jediný) token
        assertTrue(globalExpirationCalled, "Globalni expiration handler nebyl zavolan")
    }
}