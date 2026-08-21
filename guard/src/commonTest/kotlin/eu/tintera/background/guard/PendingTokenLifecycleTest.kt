package eu.tintera.background.guard

import eu.tintera.background.guard.fakes.FakeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

/**
 * Tokens from a [PendingTokenProducer] arrive on the system's schedule and die on their own
 * deadline, so both of these happen in practice:
 *
 * - a token expires before anyone asks for an execution context (nobody was working at the time),
 * - the token that started a session finishes while the session is still alive (the work it stood
 *   for is done, but other work may keep running).
 *
 * Neither may leave the environment unable to hand out a context again. That failure is invisible
 * from the outside — `acquire()` simply never returns — until every background task in the app has
 * quietly stopped running.
 */
class PendingTokenLifecycleTest {

    @Test
    fun `token that expired before anyone acquired does not block later sessions`() = runTest {
        val fixture = Fixture(testScheduler)

        // Nobody held an execution context while this one was alive, so it timed out on its own.
        val stale = FakeToken("stale")
        fixture.producer.emit(stale)
        stale.cancel()
        advanceUntilIdle()

        fixture.producer.emit(FakeToken("fresh"))

        val context = withTimeoutOrNull(5.seconds) { fixture.env.acquire() }

        assertNotNull(context, "an expired token must not be handed out as if it were alive")
        assertFalse(context.isExpired.value)

        fixture.close()
    }

    @Test
    fun `context expires when the last token finishes during the session`() = runTest {
        val fixture = Fixture(testScheduler)

        val token = FakeToken("only")
        fixture.producer.emit(token)

        val context = assertNotNull(withTimeoutOrNull(5.seconds) { fixture.env.acquire() })
        assertFalse(context.isExpired.value, "the token still covers us")

        // Released from the outside — the consumer knows the work this token stood for is done.
        // Nothing covers the session any more, so it has to end rather than run on unprotected.
        token.release()
        advanceUntilIdle()

        assertTrue(context.isExpired.value, "nothing covers the session, it must not run on")

        fixture.close()
    }

    @Test
    fun `environment recovers after the last token finishes`() = runTest {
        val fixture = Fixture(testScheduler)

        val first = FakeToken("first")
        fixture.producer.emit(first)
        assertNotNull(withTimeoutOrNull(5.seconds) { fixture.env.acquire() })

        first.cancel()
        advanceUntilIdle()

        fixture.producer.emit(FakeToken("second"))

        val context = withTimeoutOrNull(5.seconds) { fixture.env.acquire() }

        assertNotNull(context, "a new permission has to start a new session")
        assertFalse(context.isExpired.value)

        fixture.close()
    }

    /**
     * The environment gets a scope of its own rather than `runTest`'s `backgroundScope`: background
     * coroutines only make progress while the test coroutine is suspended, so an assertion right
     * after `advanceUntilIdle()` would read state that has not been updated yet and the test would
     * pass or fail for the wrong reason.
     */
    private class Fixture(scheduler: TestCoroutineScheduler) {
        private val scope = CoroutineScope(StandardTestDispatcher(scheduler))

        val producer = ManualPendingProducer(scope)

        val env = ExecutionEnvironmentFactory.create(
            scope = scope,
            tokenProducers = listOf(producer),
            config = ExecutionEnvironmentConfig(releaseDebounce = 1.seconds),
            dispatcher = StandardTestDispatcher(scheduler),
        )

        fun close() = scope.cancel()
    }

    private class ManualPendingProducer(scope: CoroutineScope) : PendingTokenProducer(scope) {
        fun emit(token: Token) = produce(token)
    }
}
