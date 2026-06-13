package eu.tintera.guard

import eu.tintera.guard.fakes.FakeToken
import eu.tintera.tasks.core.fakes.FakeTokenProducer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CompositeTokenProducerTest {

    @Test
    fun `when only one token expires global expiration is triggered`() = runTest {
        val producer = FakeTokenProducer()
        val compositeProducer = CompositeTokenProducer(
            scope = CoroutineScope(SupervisorJob()),
            dispatcher = StandardTestDispatcher(testScheduler),
            producers = listOf(producer),
            onTokenProducerRegistered = {}
        )

        var globalExpired = false
        val compositeTokenAsync = async {
            val token = compositeProducer.token().first()
            token.invokeOnPreCancel { globalExpired = true }
            token
        }
        runCurrent()

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
    fun `when multiple tokens exist global expiration waits for the last one`() = runTest {
        val producerA = FakeTokenProducer()
        val producerB = FakeTokenProducer()
        val compositeProducer = CompositeTokenProducer(
            scope = CoroutineScope(SupervisorJob()),
            dispatcher = StandardTestDispatcher(testScheduler),
            producers = listOf(producerA, producerB),
            onTokenProducerRegistered = {}
        )

        var globalExpired = false
        val compositeTokenAsync = async {
            val token = compositeProducer.token().first()
            token.invokeOnPreCancel { globalExpired = true }
            token
        }

        // Oba producenti dodají své štíty (máme dvojitý štít)
        producerA.emitToken(FakeToken("A"))
        runCurrent()
        producerB.emitToken(FakeToken("B"))
        runCurrent()

        // Umře štít A (např. 30s background task)
        producerA.simulateExpiration()
        runCurrent()

        assertFalse(globalExpired, "Global expiration should NOT trigger, Token B is still active")

        // Teprve když umře i štít B, orchestrátor to zabalí
        producerB.simulateExpiration()
        runCurrent()
        assertTrue(globalExpired, "Global expiration MUST trigger when the final token dies")
    }

    @Test
    fun `when composite token is released all active sub-tokens are released`() = runTest {
        val producerA = FakeTokenProducer()
        val producerB = FakeTokenProducer()
        val compositeProducer = CompositeTokenProducer(
            scope = this,
            dispatcher = StandardTestDispatcher(testScheduler),
            producers = listOf(producerA, producerB),
            onTokenProducerRegistered = {}
        )

        // Získáme ten náš orchestrální CompositeToken
        val compositeTokenAsync = async { compositeProducer.token().first() }
        runCurrent()

        val tokenA = FakeToken("A")
        val tokenB = FakeToken("B")
        producerA.emitToken(tokenA)
        runCurrent()
        producerB.emitToken(tokenB)
        runCurrent()

        // Byznys logika skončila, voláme release()
        val compositeToken = compositeTokenAsync.await()
        runCurrent()
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

        val compositeProducer = CompositeTokenProducer(
            scope = backgroundScope, // Poskytuje coroutines-test pro background joby
            dispatcher = dispatcher,
            producers = listOf(producer1),
            onTokenProducerRegistered = {}
        )

        // Act
        val compositeTokenAsync = async { compositeProducer.token().first() }
        launch {
            val fakeToken = producer1.emitNewToken()

            // Prvotní stav - token žije
            assertFalse(fakeToken.isReleased)

            val compositeToken = compositeTokenAsync.await()

            // Zrušíme kompozitní token
            compositeToken.release()

            // Assert - uvolnění se musí propagovat do vnitřního tokenu
            assertTrue(fakeToken.isReleased)
        }
    }

    @Test
    fun `dynamicky pridany producer po acquire je korektne sledovan`() = runTest {
        // Arrange
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val compositeProducer = CompositeTokenProducer(
            scope = backgroundScope,
            dispatcher = dispatcher,
            producers = emptyList(),// Startujeme prázdní!
            onTokenProducerRegistered = {}
        )

        val dynamicProducer = FakeTokenProducer()

        val compositeTokenAsync = async { compositeProducer.token().first() }

        launch {
            compositeProducer.registerProducer(dynamicProducer)
            val fakeToken = dynamicProducer.emitNewToken()
            val compositeToken = compositeTokenAsync.await()
            compositeToken.release()
            assertTrue(fakeToken.isReleased, "Dynamically added token should be released")
        }
    }

    @Test
    fun `globalni cancel propaguje zruseni do vsech aktivnich tokenu`() = runTest {
        // Arrange
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val producer1 = FakeTokenProducer()
        val producer2 = FakeTokenProducer()
        val compositeProducer = CompositeTokenProducer(
            scope = backgroundScope,
            dispatcher = dispatcher,
            producers = listOf(producer1, producer2),
            onTokenProducerRegistered = {}
        )

        // Act
        val compositeTokenAsync = async { compositeProducer.token().first() }

        launch {
            val token1 = producer1.emitNewToken()
            val token2 = producer2.emitNewToken()

            // Zavoláme natvrdo cancel na celém kompozitu
            compositeTokenAsync.await().release()

            assertTrue(token1.isReleased)
            assertTrue(token2.isReleased)
        }
    }

    @Test
    fun `expirace vsech vnitrnich tokenu zavola globalni expiration handler`() = runTest {
        // Arrange
        val dispatcher = StandardTestDispatcher(testScheduler)
        val producer = FakeTokenProducer()
        val compositeProducer = CompositeTokenProducer(
            scope = backgroundScope,
            dispatcher = dispatcher,
            producers = listOf(producer),
            onTokenProducerRegistered = {}
        )

        var globalExpirationCalled = false
        launch {
            val token = compositeProducer.token().first()
            token.invokeOnPreCancel {
                globalExpirationCalled = true
            }
        }

        runCurrent()

        // Získáme token, aby nebyla mapa prázdná
        producer.emitNewToken()
        runCurrent()
        assertFalse(globalExpirationCalled)

        // Act - Simulujeme, že systém (iOS) ukončil tento úkol
        producer.simulateExpiration()
        runCurrent()

        // Assert - globální handler se musel zavolat, protože to byl poslední (a jediný) token
        assertTrue(globalExpirationCalled, "Globalni expiration handler nebyl zavolan")
    }

    @Test
    fun `synchronous execution order of preCancel and onCancel is correct`() = runTest {
        var preCancelOrder = -1
        var cancelOrder = -1
        var counter = 0

        class OrderedCancelToken(
            val onCancelAction: () -> Unit
        ) : AbstractToken() {
            override val tag = "OrderedCancelToken"
            override suspend fun onRelease() {}
            override fun onCancel() {
                onCancelAction()
            }
            fun triggerExpiration() {
                finishWithCancel()
            }
        }

        val testToken = OrderedCancelToken(
            onCancelAction = {
                cancelOrder = ++counter
            }
        )

        val testProducer = TokenProducer {
            kotlinx.coroutines.flow.flowOf(testToken)
        }

        val compositeProducer = CompositeTokenProducer(
            scope = backgroundScope,
            dispatcher = StandardTestDispatcher(testScheduler),
            producers = listOf(testProducer),
            onTokenProducerRegistered = {}
        )

        val compositeToken = compositeProducer.token().first()
        compositeToken.invokeOnPreCancel {
            preCancelOrder = ++counter
        }

        // Act: trigger expiration on the underlying token
        testToken.triggerExpiration()

        println("preCancelOrder: $preCancelOrder, cancelOrder: $cancelOrder")

        // Assert: pre-cancel MUST execute before the underlying token's onCancel
        assertEquals(1, preCancelOrder, "preCancel hook of combined token should execute first")
        assertEquals(2, cancelOrder, "onCancel of the platform token should execute after preCancel hooks")
    }
}
