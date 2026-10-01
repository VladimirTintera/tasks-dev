package eu.tintera.background.tasks.runtime

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * Decides how long a lookup of something not registered yet may wait before it gives up.
 *
 * Two things hold a lookup back. The warmup window is a time limit counted from the first
 * unanswered lookup — a guess for applications that say nothing about when they register. An open
 * registration phase ([openPhase]) is the application saying so outright, and as long as one is
 * open the lookup waits regardless of the window. That matters once the window is gone: a slow
 * start can burn it before registering even begins, and the registration a lookup needs may still
 * be queued behind the very singleton that looks it up.
 */
internal open class WarmupCache(
    private val clock: Clock,
    warmupTimeout: Duration = DEFAULT_WARMUP_TIMEOUT
) {
    /**
     * How long to wait after the first lookup for registrations to settle.
     *
     * A `var` because the registry is a process-wide singleton created before Koin exists, so the
     * configured value cannot be passed through the constructor. [TasksInitializerBase] sets it
     * once at startup.
     */
    internal var warmupTimeout: Duration = warmupTimeout
    private val mutex = Mutex()

    private data class Warmup(
        val startedAt: Instant? = null,
        val consumed: Boolean = false
    )

    private val warmupDone = MutableStateFlow(Warmup())

    /** Registration phases currently open, see [eu.tintera.background.tasks.Registry.openRegistrationPhase]. */
    private val openPhases = MutableStateFlow(0)

    protected fun openPhase(): AutoCloseable {
        openPhases.update { it + 1 }

        val closed = MutableStateFlow(false)
        return AutoCloseable {
            if (closed.compareAndSet(expect = false, update = true)) openPhases.update { it - 1 }
        }
    }

    protected suspend fun <T, R : Any> MutableStateFlow<Map<T, R>>.resolveWithWarmupCheck(
        key: T
    ): R? {
        value[key]?.let { return it }

        val remainingWait = remainingWarmup()

        if (remainingWait.isPositive()) {
            withTimeoutOrNull(remainingWait) {
                first { it.containsKey(key) }[key]
            }?.let { return it }
        }

        warmupDone.update { it.copy(consumed = true) }

        // The window is over; only an open registration phase still justifies waiting. Registering
        // happens before its phase is closed, so the last state with no phase open is final.
        return combine(this, openPhases) { registrations, phases -> registrations[key] to phases }
            .first { (registration, phases) -> registration != null || phases == 0 }
            .first
    }

    private suspend fun remainingWarmup(): Duration {
        if (warmupDone.value.consumed) return Duration.ZERO

        return mutex.withLock {
            val now = clock.now()

            val updated = warmupDone.updateAndGet { current ->
                current.copy(startedAt = current.startedAt ?: now)
            }

            warmupTimeout - (now - updated.startedAt!!)
        }
    }
}

/** Default window for registrations to settle after process start. */
internal val DEFAULT_WARMUP_TIMEOUT: Duration = 5.seconds
