package eu.tintera.background.tasks.runtime

import eu.tintera.background.tasks.Tag
import eu.tintera.background.tasks.core.TagRegistration
import eu.tintera.background.tasks.registering
import eu.tintera.background.tasks.serialization.TagSerializer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * A registration phase holds lookups back for as long as someone is still registering, whatever
 * the warmup window says.
 *
 * The case behind it: an application started in the background on a slow device. A worker looked
 * the registry up right after the engine started, the application then needed more than the
 * warmup window to get its Koin going, and so the window was gone before anything was registered.
 * In the eager phase one singleton cancelled tasks by a typed tag while the singleton registering
 * that tag was still queued behind it — the lookup came back empty and the process went down.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TaskRegistryPhaseTest {

    private class LateTag : Tag

    private object LateTagSerializer : TagSerializer<LateTag> {
        override fun encodeToString(value: LateTag) = ""
        override fun decodeFromStringOrNull(value: String) = LateTag()
    }

    private fun TaskRegistry.registerLateTag() = registerTag(
        identifier = "late",
        type = LateTag::class,
        serializer = LateTagSerializer
    )

    /** The window is already gone, which is exactly when it used to break. */
    private fun registryWithoutWarmup() = TaskRegistry(
        clock = TaskRegistryTest.FakeClock(),
        warmupTimeout = Duration.ZERO
    )

    @Test
    fun `an open phase holds a lookup until the registration arrives`() = runTest {
        val registry = registryWithoutWarmup()
        val phase = registry.openRegistrationPhase()

        val lookup = async { registry.resolveTag<LateTag>(LateTag::class) }
        runCurrent()
        assertTrue(lookup.isActive)

        registry.registerLateTag()
        phase.close()

        assertNotNull(lookup.await())
    }

    @Test
    fun `a burned warmup window does not end the wait while a phase is open`() = runTest {
        val clock = TaskRegistryTest.FakeClock()
        val registry = TaskRegistry(clock = clock, warmupTimeout = 5.seconds)

        // An early worker burns the window before the application registers anything.
        val early = launch { registry.resolveTag<LateTag>("worker_identifier") }
        advanceTimeBy(5.seconds)
        clock.advanceBy(5.seconds)
        early.join()

        val phase = registry.openRegistrationPhase()
        val lookup = async { registry.resolveTag<LateTag>(LateTag::class) }
        advanceTimeBy(30.seconds)
        clock.advanceBy(30.seconds)
        assertTrue(lookup.isActive)

        registry.registerLateTag()
        phase.close()

        assertNotNull(lookup.await())
    }

    @Test
    fun `closing the last phase without the registration answers null`() = runTest {
        val registry = registryWithoutWarmup()
        val phase = registry.openRegistrationPhase()

        val lookup = async { registry.resolveTag<LateTag>(LateTag::class) }
        runCurrent()

        phase.close()

        assertNull(lookup.await())
    }

    @Test
    fun `nested phases hold the lookup until the last one is closed`() = runTest {
        val registry = registryWithoutWarmup()
        val outer = registry.openRegistrationPhase()
        val inner = registry.openRegistrationPhase()

        val lookup = async { registry.resolveTag<LateTag>(LateTag::class) }
        runCurrent()

        inner.close()
        runCurrent()
        assertTrue(lookup.isActive)

        outer.close()
        assertNull(lookup.await())
    }

    @Test
    fun `closing a phase twice does not close someone else's`() = runTest {
        val registry = registryWithoutWarmup()
        val first = registry.openRegistrationPhase()
        val second = registry.openRegistrationPhase()

        first.close()
        first.close()

        val lookup = async { registry.resolveTag<LateTag>(LateTag::class) }
        runCurrent()
        assertTrue(lookup.isActive)

        second.close()
        assertNull(lookup.await())
    }

    @Test
    fun `registering closes the phase even when the block throws`() = runTest {
        val registry = registryWithoutWarmup()

        runCatching { registry.registering { error("registration failed") } }

        var result: TagRegistration<LateTag>? = null
        val lookup = launch { result = registry.resolveTag(LateTag::class) }
        runCurrent()

        assertFalse(lookup.isActive)
        assertNull(result)
    }

    @Test
    fun `without a phase a burned window answers at once`() = runTest {
        val registry = registryWithoutWarmup()

        val before = testScheduler.currentTime
        val result = registry.resolveTag<LateTag>(LateTag::class)

        assertNull(result)
        assertEquals(before, testScheduler.currentTime)
    }
}
