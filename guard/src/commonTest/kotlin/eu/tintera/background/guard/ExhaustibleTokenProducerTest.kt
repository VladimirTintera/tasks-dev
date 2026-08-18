package eu.tintera.background.guard

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertTrue
import kotlin.test.assertNotNull


class ExhaustibleTokenProducerTest {

    @Test
    fun `emits token initially exhausts on expire and produces new token when onStarted is called`() = runTest {
        val producer = TestExhaustibleTokenProducer()
        var parentExpireCallCount = 0

        // Collect the flow with Turbine.
        producer.token().test {
            // --- PHASE 1: INITIALIZATION ---
            // The first token should arrive as soon as we subscribe.
            val firstToken = awaitItem()

            firstToken.invokeOnPreCancel { parentExpireCallCount++ }

            assertEquals(1, producer.produceCallCount, "Should produce exactly one token initially")

            // --- PHASE 2: EXHAUSTION ---
            // Simulate the system running out of time and calling onExpire().
            producer.capturedExpireCallback.invoke()

            // The state must propagate outwards, e.g. into the composite.
            assertEquals(1, parentExpireCallCount, "Parent onExpire should be triggered")

            // The producer is exhausted and emits NO further token; Turbine asserts the queue is
            // empty.
            expectNoEvents()

            // --- PHASE 3: RESURRECTION ---
            // Simulate the OS waking the application, e.g. through HealthKit.
            // a lifecycle observer zavolal onStarted()
            producer.onStarted()

            // The StateFlow flips and immediately requests and emits a new token.
            val secondToken = awaitItem()

            secondToken.invokeOnPreCancel { parentExpireCallCount++ }

            assertEquals(2, producer.produceCallCount, "Should produce a second token after resurrection")
            assertEquals(1, parentExpireCallCount, "Parent onExpire count should remain the same for now")
            assertNotSame(firstToken, secondToken, "Should be a completely new token instance")

            // Stop collecting — a StateFlow-backed flow would otherwise run forever.
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `providedObservers contains self`() {
        val producer = TestExhaustibleTokenProducer()

        // The producer offers itself as an observer.
        assertTrue(
            producer.providedObservers.contains(producer),
            "Producer should expose itself in providedObservers"
        )
    }
}