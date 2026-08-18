package eu.tintera.background.tasks.core

import eu.tintera.background.tasks.TaskLifecycleObserver
import eu.tintera.background.tasks.TaskResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlin.uuid.Uuid

internal interface ActiveTaskTracker {
    fun getActiveIds(): Set<Uuid>
}

internal class ActiveTaskTrackerImpl : ActiveTaskTracker, TaskLifecycleObserver {
    // A StateFlow doubles as a thread-safe set.
    private val activeIds = MutableStateFlow<Set<Uuid>>(emptySet())

    private fun track(id: Uuid) {
        activeIds.update { it + id }
    }

    private fun untrack(id: Uuid) {
        activeIds.update { it - id }
    }

    override fun getActiveIds(): Set<Uuid> = activeIds.value

    override fun onStarted(id: Uuid) {
        track(id)
    }

    override fun onCompleted(id: Uuid, result: TaskResult<Any>) {
        untrack(id)
    }

    override fun onCanceled(id: Uuid, reason: String?) {
        untrack(id)
    }
}