package eu.tintera.background.guard.fakes

import eu.tintera.background.guard.ExecutionContextObserver
import kotlinx.coroutines.delay
import kotlin.time.Duration

class SpyExecutionContextObserver : ExecutionContextObserver {
    var startedCount = 0
    var preReleaseCount = 0
    var preCancelCount = 0

    // Pro simulaci dlouhotrvající práce observeru (např. testování timeoutu)
    var delayInPreRelease: Duration = Duration.ZERO

    override fun onStarted() {
        startedCount++
    }

    override suspend fun onPreRelease() {
        preReleaseCount++
        if (delayInPreRelease.isPositive()) {
            delay(delayInPreRelease)
        }
    }

    override fun onPreCancel() {
        preCancelCount++
    }
}