package eu.tintera.background.guard

import app.cash.turbine.test
import eu.tintera.background.guard.fakes.FakeToken
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class ExecutionEnvironmentIntegrationTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `exhausted producer resurrects and joins immediately when another producer starts a session`() = runTest {
        val producerA = TestExhaustibleTokenProducer()
        val producerB = SimpleConstantProducer()
        val testDispatcher = StandardTestDispatcher(testScheduler)

        val env = ExecutionEnvironmentFactory.create(
            scope = backgroundScope,
            tokenProducers = listOf(producerA, producerB),
            config = ExecutionEnvironmentConfig(releaseDebounce = 1.seconds),
            dispatcher = testDispatcher
        )

        producerA.produceCallCountFlow.test {
            assertEquals(0, awaitItem(), "Producer A should not produce tokens yet")

            val context1 = env.acquire()

            assertEquals(1, awaitItem(), "Producer A should produce first token")
            assertEquals(1, producerB.produceCallCount, "Producer B should produce 1 token")

            producerA.capturedExpireCallback.invoke()
            context1.release()
            advanceTimeBy(1.seconds + 1.milliseconds)

            val context2 = env.acquire()

            assertEquals(2, awaitItem(), "Producer A created new token after producer B token appeared!")

            assertEquals(2, producerB.produceCallCount, "Producer B should produce 2 tokens in total")

            context2.release()

            cancelAndIgnoreRemainingEvents()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `exhausted producer resurrects and joins immediately when another async producer starts a session`() = runTest {
        val producerA = TestExhaustibleTokenProducer()
        val producerB = SystemProducer()
        val testDispatcher = StandardTestDispatcher(testScheduler)

        val env = ExecutionEnvironmentFactory.create(
            scope = backgroundScope,
            tokenProducers = listOf(producerA, producerB),
            config = ExecutionEnvironmentConfig(releaseDebounce = 1.seconds),
            dispatcher = testDispatcher
        )

        producerA.produceCallCountFlow.test {
            assertEquals(0, awaitItem(), "Producer A should not produce tokens yet")
            assertEquals(0, producerB.produceCallCount, "Producer B should not produce tokens yet")

            // --- RELACE 1 ---
            val context1 = env.acquire()

            assertEquals(1, awaitItem(), "Producer A should produce first token")
            assertEquals(0, producerB.produceCallCount, "Producer B should not produce tokens yet")

            producerA.capturedExpireCallback.invoke()
            context1.release()
            advanceTimeBy(1.seconds + 1.milliseconds)

            val context2 = withTimeoutOrNull(10.seconds) {
                env.acquire()
            }

            assertNull(context2, "context2 should be null. No token is alive, producer A is exhausted")

            producerB.createToken()

            val context3 = env.acquire()

            assertEquals(2, awaitItem(), "Producer A created new token after producer B token appeared!")

            context3.release()
            advanceTimeBy(1.seconds + 1.milliseconds)

            cancelAndIgnoreRemainingEvents()
        }
    }
}

class SystemProducer : TokenProducer {

    private val token = MutableStateFlow<Token?>(null)
    var produceCallCount = 0

    fun createToken() = token.update { FakeToken("SystemToken") }

    override fun token(): Flow<Token> = token.filterNotNull().onEach {
        println("creating token")
        produceCallCount++
    }

}

class SimpleConstantProducer : TokenProducer {
    var produceCallCount = 0
    override fun token(): Flow<Token> = flow {
        produceCallCount++
        emit(FakeToken("Fake B"))
    }
}