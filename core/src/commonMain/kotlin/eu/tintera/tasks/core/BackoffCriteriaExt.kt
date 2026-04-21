package eu.tintera.tasks.core

import eu.tintera.tasks.BackoffCriteria
import eu.tintera.tasks.BackoffPolicy
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

fun BackoffCriteria.calculate(retryCount: Int): Duration {

    val baseDelay = delay.coerceAtLeast(MIN_BACKOFF_DELAY)

    return when (backoffPolicy) {
        BackoffPolicy.Linear -> {
            (baseDelay * (retryCount + 1)).coerceAtMost(MAX_BACKOFF_DELAY)
        }

        BackoffPolicy.Exponential -> {
            var currentDelay = baseDelay
            // Změna: Opakujeme o jedenkrát méně. Pro attempt = 1 se repeat neprovede vůbec.
            repeat(retryCount) {
                currentDelay *= 2
                if (currentDelay >= MAX_BACKOFF_DELAY) {
                    return MAX_BACKOFF_DELAY
                }
            }
            currentDelay
        }
    }
}

val BackoffCriteria.Companion.DEFAULT
    get() = BackoffCriteria(
        backoffPolicy = BackoffPolicy.Exponential,
        delay = DEFAULT_BACKOFF_DELAY
    )

val MINIMAL_REPEAT_INTERVAL: Duration get() = 15.minutes
val DEFAULT_BACKOFF_DELAY: Duration get() = 30.seconds
val MAX_BACKOFF_DELAY: Duration = 5.hours
val MIN_BACKOFF_DELAY: Duration = 10.seconds