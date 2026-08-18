package eu.tintera.background.guard

import eu.tintera.background.guard.fakes.FakeToken
import eu.tintera.background.tasks.core.fakes.FakeTokenProducer
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

        // Simulate the token expiring.
        producer.simulateExpiration()

        runCurrent()

        // It was the only token, so the orchestrator must trigger the global teardown.
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

        // Both producers supply a token, so two are held.
        producerA.emitToken(FakeToken("A"))
        runCurrent()
        producerB.emitToken(FakeToken("B"))
        runCurrent()

        // Token A dies, e.g. a 30s background task.
        producerA.simulateExpiration()
        runCurrent()

        assertFalse(globalExpired, "Global expiration should NOT trigger, Token B is still active")

        // Only once token B dies too does the orchestrator give up.
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

        // Take the orchestrating CompositeToken.
        val compositeTokenAsync = async { compositeProducer.token().first() }
        runCurrent()

        val tokenA = FakeToken("A")
        val tokenB = FakeToken("B")
        producerA.emitToken(tokenA)
        runCurrent()
        producerB.emitToken(tokenB)
        runCurrent()

        // The work is done, so release().
        val compositeToken = compositeTokenAsync.await()
        runCurrent()
        compositeToken.release()
        runCurrent()

        // Every held token must be told to release.
        assertTrue(tokenA.isReleased, "Token A should be released")
        assertTrue(tokenB.isReleased, "Token B should be released")
        assertFalse(tokenA.isCanceled, "Token A should not be canceled")
        assertFalse(tokenB.isCanceled, "Token B should not be canceled")
    }

    @Test
    fun `acquire picks up tokens from the initial producer`() = runTest {
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

            // Initial state: the token is alive.
            assertFalse(fakeToken.isReleased)

            val compositeToken = compositeTokenAsync.await()

            // Cancel the composite token.
            compositeToken.release()

            // Assert: the release propagates to the inner token.
            assertTrue(fakeToken.isReleased)
        }
    }

    @Test
    fun `a producer added after acquire is tracked correctly`() = runTest {
        // Arrange
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val compositeProducer = CompositeTokenProducer(
            scope = backgroundScope,
            dispatcher = dispatcher,
            producers = emptyList(),// start with nothing
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
    fun `a global cancel propagates to every active token`() = runTest {
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

            // Cancel the whole composite outright.
            compositeTokenAsync.await().release()

            assertTrue(token1.isReleased)
            assertTrue(token2.isReleased)
        }
    }

    @Test
    fun `expiring every inner token invokes the global expiration handler`() = runTest {
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

        // Take a token so the map is not empty.
        producer.emitNewToken()
        runCurrent()
        assertFalse(globalExpirationCalled)

        // Act: simulate the system (iOS) ending this task.
        producer.simulateExpiration()
        runCurrent()

        // Assert: the global handler ran, because that was the last (and only) token.
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

    @Test
    fun `when multiple tokens are cancelled concurrently global expiration is still triggered`() = runTest {
        val producerA = FakeTokenProducer()
        val producerB = FakeTokenProducer()
        val compositeProducer = CompositeTokenProducer(
            scope = backgroundScope,
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
        runCurrent()

        val tokenA = producerA.emitNewToken()
        runCurrent()

        val tokenB = producerB.emitNewToken()
        runCurrent()

        println("Before cancel - activeTokens: ${tokenA.state.value}, ${tokenB.state.value}")

        // Act: cancel both tokens concurrently (before any coroutines run to remove them from activeTokens)
        tokenA.cancel()
        println("After tokenA cancel - globalExpired: $globalExpired")
        tokenB.cancel()
        println("After tokenB cancel - globalExpired: $globalExpired")

        runCurrent()

        println("After runCurrent - globalExpired: $globalExpired")

        // Assert: global expiration must be triggered because all tokens are cancelled
        assertTrue(globalExpired, "Global expiration MUST trigger when all tokens are cancelled, even concurrently")
    }
}


