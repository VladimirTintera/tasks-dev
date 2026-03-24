package eu.tintera.tasks.core

import eu.tintera.tasks.EventBus
import eu.tintera.tasks.core.locks.Token
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import platform.UIKit.UIApplication
import platform.UIKit.UIBackgroundTaskInvalid
import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.time.Duration.Companion.seconds


/**
 * Manages the lifecycle of a background task on iOS.
 *
 * This class listens to the application's background state and automatically acquires
 * or releases a `UIBackgroundTask` to ensure the process has time to complete its work.
 *
 * @property isBackground A [StateFlow] indicating whether the application is currently in the background.
 * @property expirationHandler Callback invoked by iOS when the background time is about to expire.
 */
internal class LifecycleToken(
    private val isBackground: StateFlow<Boolean>,
    scope: CoroutineScope,
    private val expirationHandler: () -> Unit,
    private val bgTaskManager: BgTaskManager
) : Token {

    // -1L = Invalid, -2L = Terminated, > 0 = Active Task ID
    private val taskIdentifier = AtomicLong(UIBackgroundTaskInvalid.toLong())

    private val job = scope.launch {
        isBackground.collect { background ->
            if (background) {
                acquire()
            } else {
                releaseActiveTask(toState = UIBackgroundTaskInvalid.toLong())
            }
        }
    }

    private fun acquire() {
        val current = taskIdentifier.load()
        if (current != UIBackgroundTaskInvalid.toLong()) return

        val newIdentifier = UIApplication.sharedApplication.beginBackgroundTaskWithExpirationHandler {
            expirationHandler()
        }

        // Atomický zápis nového tasku. Pokud se povede, logujeme úspěch.
        if (taskIdentifier.compareAndSet(UIBackgroundTaskInvalid.toLong(), newIdentifier.toLong())) {
            EventBus.send(TAG, "Background task acquired: $newIdentifier")
        } else {
            // Pokud se stav změnil (už jsme v popředí nebo je token mrtvý), okamžitě rušíme.
            UIApplication.sharedApplication.endBackgroundTask(newIdentifier)
            EventBus.send(TAG, "Discarded late background task (state changed): $newIdentifier")
        }
    }

    private fun releaseActiveTask(toState: Long) {
        while (true) {
            val current = taskIdentifier.load()

            // 1. Pokud už je token trvale mrtvý, nemáme co řešit.
            if (current == RELEASED_STATE) break

            // 2. Pokud chceme pauzovat do popředí (toState == Invalid),
            // ale my už v Invalid stavu jsme, není co ukončovat.
            if (toState == UIBackgroundTaskInvalid.toLong() && current == UIBackgroundTaskInvalid.toLong()) break

            // 3. Pokus o zápis nového stavu
            if (taskIdentifier.compareAndSet(current, toState)) {
                // Pokud jsme předtím drželi skutečný task (> 0), musíme ho ukončit u iOS
                if (current > 0L) {
                    UIApplication.sharedApplication.endBackgroundTask(current.toULong())
                    EventBus.send(TAG, "Successfully ended active background task ID: $current")
                }
                break
            }
        }
    }

    /**
     * Releases the token and triggers a background task evaluation.
     *
     * This method cancels the background state observer, attempts to schedule the next
     * pending task within a 2-second timeout, and ensures any active iOS background
     * task is properly ended.
     */
    override suspend fun release() {
        job.cancel()

        EventBus.send(TAG, "Releasing. isBackground=${isBackground.value}")

        // Drobná úprava: Chceme plánovat vždy, když se appka vypíná korektně,
        // i když zrovna náhodou byla v popředí (current <= 0).
        // Takže kontrolu 'current > 0' jsem dal pryč, ať se fronta nevylidní!
        withTimeoutOrNull(2.seconds) {
            bgTaskManager.evaluateAndScheduleNext()
        }

        finish()
    }

    /**
     * Cancels the token immediately.
     *
     * Stops observing background state changes and cleans up any active background tasks.
     */
    override fun cancel() {
        job.cancel()
        EventBus.send(TAG, "Canceling. isBackground=${isBackground.value}")
        finish()
    }

    private fun finish() {
        releaseActiveTask(toState = RELEASED_STATE)
    }

    override fun toString(): String {
        return "LifecycleToken(isBackground=${isBackground.value})"
    }

    companion object {
        private const val RELEASED_STATE = -2L
        private const val TAG = "LifecycleToken"
    }
}