package eu.tintera.background.tasks.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlin.uuid.Uuid

internal interface ActiveTaskTracker {
    fun track(id: Uuid)
    fun untrack(id: Uuid)
    fun getActiveIds(): Set<Uuid>
}

internal class ActiveTaskTrackerImpl : ActiveTaskTracker {
    // Použijeme StateFlow jako elegantní thread-safe Set
    private val activeIds = MutableStateFlow<Set<Uuid>>(emptySet())

    override fun track(id: Uuid) {
        activeIds.update { it + id }
    }

    override fun untrack(id: Uuid) {
        activeIds.update { it - id }
    }

    override fun getActiveIds(): Set<Uuid> = activeIds.value
}