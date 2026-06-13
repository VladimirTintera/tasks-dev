package eu.tintera.background.tasks.core

import eu.tintera.background.tasks.BackoffCriteria
import eu.tintera.background.tasks.BackoffPolicy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class BackoffCriteriaTest {

    @Test
    fun testLinearBackoff() {
        val safeDelay = 1.minutes
        val criteria = BackoffCriteria(
            backoffPolicy = BackoffPolicy.Linear,
            delay = safeDelay
        )

        // 0 -> safeDelay * 1
        assertEquals(safeDelay, criteria.calculate(0))

        // 1 -> safeDelay * 2
        assertEquals(safeDelay * 2, criteria.calculate(1))

        // 2 -> safeDelay * 3
        assertEquals(safeDelay * 3, criteria.calculate(2))
    }

    @Test
    fun testExponentialBackoff() {
        val safeDelay = 1.minutes
        val criteria = BackoffCriteria(
            backoffPolicy = BackoffPolicy.Exponential,
            delay = safeDelay
        )

        // 0 -> safeDelay * 2^0 = safeDelay
        assertEquals(safeDelay, criteria.calculate(0))

        // 1 -> safeDelay * 2^1 = safeDelay * 2
        assertEquals(safeDelay * 2, criteria.calculate(1))

        // 2 -> safeDelay * 2^2 = safeDelay * 4
        assertEquals(safeDelay * 4, criteria.calculate(2))

        // 3 -> safeDelay * 2^3 = safeDelay * 8
        assertEquals(safeDelay * 8, criteria.calculate(3))
    }

    @Test
    fun testMaxBackoffLimit() {


        val criteria = BackoffCriteria(
            backoffPolicy = BackoffPolicy.Exponential,
            delay = 1.hours
        )

        // 1h * 2^10 je hodně, určitě přesáhne MAX_BACKOFF_DELAY
        val result = criteria.calculate(10)

        assertEquals(MAX_BACKOFF_DELAY, result)
    }

    @Test
    fun testMinBackoffLimit() {
        val criteria = BackoffCriteria(
            backoffPolicy = BackoffPolicy.Linear,
            delay = 1.seconds // Velmi malý delay
        )

        // Mělo by vrátit MIN_BACKOFF_DELAY, protože 1s * 1 < MIN_BACKOFF_DELAY
        val result = criteria.calculate(0)
        assertEquals(MIN_BACKOFF_DELAY, result)
    }
}