package eu.tintera.background.tasks.runtime

import kotlinx.coroutines.flow.MutableStateFlow
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

    protected suspend fun <T, R> MutableStateFlow<Map<T, R>>.resolveWithWarmupCheck(
        key: T
    ): R? {

        if (warmupDone.value.consumed) return value[key]

        val remainingWait = mutex.withLock {
            val now = clock.now()

            val updated = warmupDone.updateAndGet { current ->
                current.copy(startedAt = current.startedAt ?: now)
            }

            val elapsed = now - updated.startedAt!!
            warmupTimeout - elapsed
        }

        return if (remainingWait.isPositive()) {

            val value = withTimeoutOrNull(remainingWait) {
                first { it.containsKey(key) }[key]
            }

            if (value == null) warmupDone.update { it.copy(consumed = true) }
            value
        } else {

            warmupDone.update { it.copy(consumed = true) }

            value[key]
        }
    }
}

/** Default window for registrations to settle after process start. */
internal val DEFAULT_WARMUP_TIMEOUT: Duration = 5.seconds
