package eu.tintera.background.tasks.core

import eu.tintera.background.guard.ExecutionContextObserver
import eu.tintera.background.tasks.State
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

internal class OrphanTaskSweeper(
    private val repository: OrphanTaskSweeperRepository,
    private val scope: ApplicationScope,
    private val dispatchers: AppDispatchers,
    private val activeTaskTracker: ActiveTaskTracker, // shared tracker
    private val appStateObserver: AppStateObserver,
) : ExecutionContextObserver {

    init {
        // 1. Cold Start Sweep
        scope.launch(dispatchers.io) {
            runSmartSweep("Cold Start")

            // 2. Foreground Sweep
            appStateObserver.isBackground.filter { !it }.collect {
                runSmartSweep("Foreground Transition")
            }
        }
    }

    // 3. Background Resurrection Sweep
    override fun onStarted() {
        scope.launch(dispatchers.io) {
            runSmartSweep("Guard Session Started")
        }
    }

    private suspend fun runSmartSweep(trigger: String) {
        val activelyRunningIds = activeTaskTracker.getActiveIds()

        repository.resetState(
            from = State.Running,
            to = State.Enqueued,
            excludedIds = activelyRunningIds
        )
    }
}