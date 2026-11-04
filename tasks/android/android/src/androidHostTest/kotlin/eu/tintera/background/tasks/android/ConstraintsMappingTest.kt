package eu.tintera.background.tasks.android

import androidx.work.NetworkType
import eu.tintera.background.tasks.Constraints
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The invariant under test is a regression: device idle used to imply a network connection, so a
 * purely local idle task never ran offline. Each constraint has to stand on its own.
 */
class ConstraintsMappingTest {

    @Test
    fun noConstraintsRequireNothing() {
        val work = Constraints.EMPTY.toWorkConstraints()

        assertEquals(NetworkType.NOT_REQUIRED, work.requiredNetworkType)
        assertFalse(work.requiresDeviceIdle())
    }

    @Test
    fun deviceIdleDoesNotRequireNetwork() {
        val work = Constraints(requiresDeviceIdle = true).toWorkConstraints()

        assertEquals(NetworkType.NOT_REQUIRED, work.requiredNetworkType)
        assertTrue(work.requiresDeviceIdle())
    }

    @Test
    fun networkDoesNotRequireDeviceIdle() {
        val work = Constraints(requiresNetwork = true).toWorkConstraints()

        assertEquals(NetworkType.CONNECTED, work.requiredNetworkType)
        assertFalse(work.requiresDeviceIdle())
    }

    @Test
    fun bothConstraintsApplyTogether() {
        val work = Constraints(requiresNetwork = true, requiresDeviceIdle = true).toWorkConstraints()

        assertEquals(NetworkType.CONNECTED, work.requiredNetworkType)
        assertTrue(work.requiresDeviceIdle())
    }
}
