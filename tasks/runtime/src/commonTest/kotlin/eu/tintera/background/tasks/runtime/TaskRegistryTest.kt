package eu.tintera.background.tasks.runtime

import eu.tintera.background.tasks.Tag
import eu.tintera.background.tasks.core.TagRegistration
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlin.test.*
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class TaskRegistryTest {

    // Simple FakeClock for advancing time manually in tests.
    class FakeClock(var currentTime: Instant = Instant.fromEpochMilliseconds(0)) : Clock {
        override fun now(): Instant = currentTime
        fun advanceBy(duration: Duration) {
            currentTime += duration
        }
    }

    private lateinit var clock: FakeClock
    private lateinit var registry: TaskRegistry

    @BeforeTest
    fun setup() {
        clock = FakeClock()
        // TaskRegistry is a class rather than an object so state is not shared between tests.
        registry = TaskRegistry(clock = clock)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `resolve missing tag starts 5s warmup and returns null when time expires`() = runTest {
        // Arrange
        var result: TagRegistration<*>? = null

        // Act: run resolve in a separate coroutine, since it suspends.
        val job = launch {
            result = registry.resolveTag<MyDummyTag>("missing_tag")
        }

        // Assert: nothing happens after 2 seconds, the coroutine is still running.
        advanceTimeBy(2.seconds)
        clock.advanceBy(2.seconds)
        assertTrue(job.isActive)
        assertNull(result)

        // Assert: after another 3 seconds (5s total) the timeout expires.
        advanceTimeBy(3.seconds)
        clock.advanceBy(3.seconds)

        // The coroutine should finish and return null.
        job.join()
        assertNull(result)
    }

    @Test
    fun `concurrent resolve requests share the same warmup window`() = runTest {
        // Arrange
        var result1: TagRegistration<*>? = null
        var result2: TagRegistration<*>? = null

        // Act 1: the first request starts the warmup window at 0s.
        val job1 = launch { result1 = registry.resolveTag<MyDummyTag>("tag_1") }

        // Advance by 3 seconds.
        advanceTimeBy(3.seconds)
        clock.advanceBy(3.seconds)

        // Act 2: a second request arrives at 3s — only 2 seconds of the window are left.
        val job2 = launch { result2 = registry.resolveTag<MyDummyTag>("tag_2") }

        // Advance by the remaining 2 seconds (5s from the start).
        advanceTimeBy(2.seconds)
        clock.advanceBy(2.seconds)

        // Assert: BOTH jobs must finish now. Job 2 did not wait 5 seconds of its own — it rode
        // along in job 1's window.
        job1.join()
        job2.join()
        assertNull(result1)
        assertNull(result2)
    }

    @Test
    fun `after warmup is consumed resolving missing tags is instant`() = runTest {
        // Arrange: deliberately exhaust the warmup window.
        val job = launch { registry.resolveTag<MyDummyTag>("trigger_warmup") }
        advanceTimeBy(5.seconds)
        clock.advanceBy(5.seconds)
        job.join()

        // Act: look up another missing tag and measure VIRTUAL time.
        var result: TagRegistration<*>? = null

        val virtualTimeBefore = testScheduler.currentTime // nebo jen currentTime
        result = registry.resolveTag<MyDummyTag>("another_missing_tag")
        val virtualTimeAfter = testScheduler.currentTime

        // Assert:
        assertNull(result)
        // Virtual time must not have moved at all — the function did not suspend.
        assertEquals(virtualTimeBefore, virtualTimeAfter)
    }
}

// Dummy class for testing.
class MyDummyTag : Tag
/**
 * The warmup window has to be extendable: an application with a slow cold start — a background
 * wake-up on a low-end device — may not register its handlers within the default 5 seconds.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TaskRegistryWarmupTimeoutTest {

    @Test
    fun `a longer warmup window holds resolve past the default 5s`() = runTest {
        val clock = TaskRegistryTest.FakeClock()
        val registry = TaskRegistry(clock = clock, warmupTimeout = 20.seconds)

        val job = launch { registry.resolveTag<MyDummyTag>("missing") }

        // With the default setting it would have given up after 5 seconds.
        advanceTimeBy(5.seconds)
        clock.advanceBy(5.seconds)
        assertTrue(job.isActive)

        advanceTimeBy(15.seconds)
        clock.advanceBy(15.seconds)
        job.join()
        assertFalse(job.isActive)
    }
}
