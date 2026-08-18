package eu.tintera.background.guard

import eu.tintera.background.guard.fakes.SpyExecutionContextObserver
import eu.tintera.background.guard.fakes.FakeTokenProvider
import eu.tintera.background.guard.fakes.FakeToken
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

val defaultReleaseDebounce = 1.5.seconds

internal fun TestScope.executionContextProvider(
    tokenProducer: TokenProducer,
    releaseDebounce: Duration = defaultReleaseDebounce
) = SharedExecutionContextProvider(
    tokenProducer = tokenProducer,
    scope = CoroutineScope(SupervisorJob()),
    dispatcher = StandardTestDispatcher(testScheduler),
    config = ExecutionEnvironmentConfig(releaseDebounce),
    lifecycleObserver = object : ExecutionContextObserver {}
)

class SharedExecutionContextProviderTest {

    @Test
    fun `when first token is acquired system lock is acquired`() = runTest {
        val fakeSystemLock = FakeTokenProvider()
        val wakeLock = executionContextProvider(fakeSystemLock)

        val token = wakeLock.acquire()

        assertEquals(1, fakeSystemLock.acquireCount)
        assertEquals(0, fakeSystemLock.releaseCount)
        assertFalse(token.isExpired.value)
    }

    @Test
    fun `when second token is acquired system lock is NOT acquired again`() = runTest {
        val fakeSystemLock = FakeTokenProvider()
        val wakeLock = executionContextProvider(fakeSystemLock)

        wakeLock.acquire()
        wakeLock.acquire()

        assertEquals(1, fakeSystemLock.acquireCount)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `when acquire is called during debounce system lock is not released`() = runTest {
        val fakeSystemLock = FakeTokenProvider()
        val wakeLock = executionContextProvider(fakeSystemLock)

        val token1 = wakeLock.acquire()
        token1.release() // starts the 1.5s countdown

        // Move to just before the release debounce expires; the lock must still be held.
        advanceTimeBy(defaultReleaseDebounce - 1.milliseconds)

        assertEquals(0, fakeSystemLock.releaseCount)

        // Another caller arrives, so the debounce must be cancelled.
        wakeLock.acquire()

        // Advance another 2 seconds. Had the debounce still been running, it would release now.
        advanceTimeBy(defaultReleaseDebounce + 1.milliseconds)

        // The lock must still be held — token2 holds it.
        assertEquals(0, fakeSystemLock.releaseCount)
    }


    @Test
    fun `when all tokens are released system lock is released`() = runTest {
        val fakeSystemLock = FakeTokenProvider()
        val wakeLock = executionContextProvider(fakeSystemLock)

        val token1 = wakeLock.acquire()
        val token2 = wakeLock.acquire()

        token1.release()
        assertEquals(0, fakeSystemLock.releaseCount, "Released count must be 0")

        token2.release()

        advanceTimeBy(defaultReleaseDebounce - 1.milliseconds)

        assertEquals(0, fakeSystemLock.releaseCount, "Released count must still be 1 (debounce)")

        advanceTimeBy(10.minutes)

        assertEquals(1, fakeSystemLock.releaseCount, "Released count must be 1")
    }

    @Test
    fun `when system lock expires all tokens are marked expired`() = runTest {
        val fakeSystemLock = FakeTokenProvider()
        val wakeLock = executionContextProvider(fakeSystemLock)

        val token1 = wakeLock.acquire()
        val token2 = wakeLock.acquire()

        // Simulate expiration
        fakeSystemLock.triggerExpiration()

        assertTrue(token1.isExpired.value)
        assertTrue(token2.isExpired.value)

        // Also check that cancel was called on the system lock
        assertEquals(1, fakeSystemLock.cancelCount)
    }

    @Test
    fun `when expired releasing tokens does not crash or double release`() = runTest {

        val tokenProvider = FakeTokenProvider()
        val executionContextProvider = executionContextProvider(tokenProvider)

        val executionContext = executionContextProvider.acquire()

        tokenProvider.triggerExpiration()

        // Release after expiration should be safe and ignored
        executionContext.release()

        // Release count should still be 0 because it was cancelled, not released normally
        assertEquals(0, tokenProvider.releaseCount)
        assertEquals(1, tokenProvider.cancelCount)
    }

    @Test
    fun `after expiration new acquire starts fresh`() = runTest {
        val fakeSystemLock = FakeTokenProvider()
        val wakeLock = executionContextProvider(fakeSystemLock)

        val token1 = wakeLock.acquire()
        fakeSystemLock.triggerExpiration()

        // Old token is expired
        assertTrue(token1.isExpired.value, "Token must be expired")

        // New acquire should start a new system lock
        val token2 = wakeLock.acquire()

        assertEquals(2, fakeSystemLock.acquireCount) // 1st expired, 2nd started
        assertFalse(token2.isExpired.value)

        token2.release()

        advanceTimeBy(defaultReleaseDebounce + 1.milliseconds)

        assertEquals(1, fakeSystemLock.releaseCount) // Only the second one was released normally
    }

    @Test
    fun `orphaned token release does not affect new session`() = runTest {
        val fakeSystemLock = FakeTokenProvider()
        val wakeLock = executionContextProvider(fakeSystemLock)

        // 1. Acquire first token
        val tokenA = wakeLock.acquire()
        assertEquals(1, fakeSystemLock.acquireCount)

        // 2. Simulate expiration, making tokenA an orphan
        fakeSystemLock.triggerExpiration()
        assertTrue(tokenA.isExpired.value)
        assertEquals(1, fakeSystemLock.cancelCount, "Canceled count must be 1")

        // 3. Acquire a new token, creating a new session
        val tokenB = wakeLock.acquire()
        assertEquals(2, fakeSystemLock.acquireCount)
        assertFalse(tokenB.isExpired.value)

        // 4. Release the orphaned tokenA
        tokenA.release()

        // 5. CRUCIAL: Assert that the new system lock was NOT released
        assertEquals(0, fakeSystemLock.releaseCount)

        // 6. Release the valid tokenB
        tokenB.release()

        // 7. Assert that the new system lock is now correctly released
        advanceTimeBy(defaultReleaseDebounce + 1.milliseconds)
        assertEquals(1, fakeSystemLock.releaseCount)
    }

    @Test
    fun `when system expires lock instantly during acquire token is cancelled immediately`() = runTest {
        // Arrange
        val fakeSystemLock = FakeTokenProvider().apply {
            simulateInstantExpiration = true // the nasty scenario
        }
        val wakeLock = executionContextProvider(fakeSystemLock)

        // Act
        // Acquire the lock; because of the flag it expires before the function returns.
        val token = wakeLock.acquire()

        // Assert
        assertEquals(1, fakeSystemLock.acquireCount, "Exactly one system request expected")

        // The expiration handler ran BEFORE the token was stored in the session, so the callback
        // could not cancel it — the defensive check after storing did.
        assertEquals(0, fakeSystemLock.releaseCount, "The defensive check must not release an already cancelled token")
        assertEquals(1, fakeSystemLock.cancelCount, "The regular cancel runs on expiration")

        // The returned token must report itself as expired straight away.
        assertTrue(token.isExpired.value, "The token must be expired immediately")

        // Act 2
        // Calling release now must do nothing at all, the token is already cancelled.
        token.release()

        // Assert 2
        assertEquals(0, fakeSystemLock.releaseCount, "The release count must stay at 0")
        assertEquals(1, fakeSystemLock.cancelCount, "Cancelling an already cancelled token must not hit the system again")
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `when coroutine is cancelled system wake lock is still released`() = runTest {
        // 1. Arrange
        val fakePlatformProvider = FakeTokenProvider()
        val countingProvider = executionContextProvider(
            tokenProducer = fakePlatformProvider,
            releaseDebounce = Duration.ZERO
        )

        // 2. Start a job that acquires the lock and then sleeps, standing in for real work.
        val job = launch {
            val token = countingProvider.acquire()
            token.use {
                // Normally the coroutine would work here; the sleep lets the test cancel it from
                // the outside.
                delay(5000)
            }
        }

        // Advance so the coroutine starts and acquires the lock.
        runCurrent()

        // The lock really was requested at the system level.
        assertEquals(1, fakePlatformProvider.acquireCount, "Acquire expected exactly once")
        assertEquals(0, fakePlatformProvider.releaseCount, "Release must NOT have been called yet")

        // 3. ACT: cancel the coroutine outright — stands in for an expiration or a deleted task.
        job.cancelAndJoin()

        // 4. ASSERT: the critical point. Without withContext(NonCancellable) the release count
        //    would be 0 and this fails.
        assertEquals(1, fakePlatformProvider.releaseCount, "Release MUST happen even after the coroutine is cancelled")
    }

    @Test
    fun `when system expires task immediately during acquire token is cancelled`() = runTest {
        val fakeProvider = FakeTokenProvider().apply {
            simulateInstantExpiration = true
        }
        val countingProvider = executionContextProvider(fakeProvider)

        // Akce
        val token = countingProvider.acquire()

        // Assert: the lock knows it is dead.
        assertTrue(token.isExpired.value, "The token should be expired straight away")

        // Assert: the defensive code cancelled the invalid token.
        assertEquals(0, fakeProvider.releaseCount)
        assertEquals(1, fakeProvider.cancelCount)
    }

    @Test
    fun `a successful acquire and release invokes onStarted and then onPreRelease after the debounce`() = runTest {
        // Arrange
        // StandardTestDispatcher gives full control over virtual time.
        val tokenProvider = FakeTokenProvider()
        val observer = SpyExecutionContextObserver()

        val contextProvider = SharedExecutionContextProvider(
            tokenProducer = tokenProvider,
            scope = backgroundScope, // provided by runTest, cleaned up automatically
            dispatcher = StandardTestDispatcher(testScheduler),
            config = ExecutionEnvironmentConfig(releaseDebounce = 5.seconds),
            lifecycleObserver = observer
        )

        // Act - Start
        val executionContext = contextProvider.acquire()

        // Assert - Start
        assertEquals(1, observer.startedCount, "onStarted melo byt zavolano presne jednou")
        assertEquals(0, tokenProvider.releaseCount)

        // Act: process and release.
        executionContext.release()

        // Right after release the observer must not see onPreRelease yet, because of the debounce.
        assertEquals(0, observer.preReleaseCount)
        assertEquals(0, tokenProvider.releaseCount)

        // Advance past the debounce limit (5 seconds).
        advanceTimeBy(5001)

        // Assert - Konec
        assertEquals(1, observer.preReleaseCount, "onPreRelease melo byt zavolano po uplynuti debounce")
        assertEquals(1, tokenProvider.releaseCount, "Systemovy token mel byt na konci uvolnen")
    }

    @Test
    fun `an instant system expiration invokes onPreCancel and cancels the token`() = runTest {
        // Arrange
        val tokenProvider = FakeTokenProvider()
        val observer = SpyExecutionContextObserver()

        val contextProvider = SharedExecutionContextProvider(
            tokenProducer = tokenProvider,
            scope = backgroundScope,
            dispatcher = StandardTestDispatcher(testScheduler),
            config = ExecutionEnvironmentConfig(releaseDebounce = 5.seconds),
            lifecycleObserver = observer
        )

        // Nastartujeme kontext
        contextProvider.acquire()
        assertEquals(1, observer.startedCount)

        // Act: the system suddenly reports that time is up.
        tokenProvider.triggerExpiration()

        // Assert
        assertEquals(1, observer.preCancelCount, "onPreCancel melo byt okamzite zavolano")
        assertEquals(0, observer.preReleaseCount, "onPreRelease must not be called on expiration")
        assertEquals(1, tokenProvider.cancelCount, "Systemovy token mel byt zrusen (cancel)")
        assertEquals(0, tokenProvider.releaseCount)
    }

    @Test
    fun `the system token is released even when onPreRelease outlasts the timeout`() = runTest {
        // Arrange
        val tokenProvider = FakeTokenProvider()
        val observer = SpyExecutionContextObserver().apply {
            // The observer blocks for 10 seconds, while withTimeoutOrNull only allows 2.
            delayInPreRelease = 10.seconds
        }

        val contextProvider = SharedExecutionContextProvider(
            tokenProducer = tokenProvider,
            scope = backgroundScope,
            dispatcher = StandardTestDispatcher(testScheduler),
            config = ExecutionEnvironmentConfig(releaseDebounce = 5.seconds),
            lifecycleObserver = observer
        )

        val executionContext = contextProvider.acquire()
        executionContext.release()

        // Advance 5 seconds for the debounce plus 2 for the provider's withTimeoutOrNull.
        advanceTimeBy((5000 + 2001).milliseconds)

        // Assert
        assertEquals(1, observer.preReleaseCount, "Observeruv onPreRelease mel byt spusten")
        assertEquals(
            1,
            tokenProvider.releaseCount,
            "The system token MUST be released even when an observer hangs"
        )
    }

    @Test
    fun `when acquire is suspended waiting for token release of old token is not blocked`() = runTest {
        var shouldSuspend = false
        val fakeToken = FakeToken()
        val customProducer = TokenProducer {

            flow {
                if (shouldSuspend) {
                    awaitCancellation()
                } else {
                    emit(fakeToken)
                }
            }
        }

        val wakeLock = executionContextProvider(
            tokenProducer = customProducer,
            releaseDebounce = Duration.ZERO
        )

        // 1. Acquire first token
        val token1 = wakeLock.acquire()

        // 2. Expire the first session
        fakeToken.cancel()
        assertTrue(token1.isExpired.value)

        // 3. Set producer to suspend and start a second acquire in background, which will try to start a new session and suspend
        shouldSuspend = true
        val secondAcquireJob = launch {
            wakeLock.acquire()
        }
        runCurrent()

        try {
            // 4. Try to release the old token1. It should succeed immediately (without waiting for the second acquire to finish)
            val releaseCompleted = withTimeoutOrNull(2.seconds) {
                token1.release()
                true
            }

            // In the buggy implementation, releaseCompleted will be null because the mutex is locked by secondAcquireJob
            assertTrue(releaseCompleted ?: false, "Release of old token should not be blocked by pending acquire")
        } finally {
            secondAcquireJob.cancelAndJoin()
        }
    }
}




