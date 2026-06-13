package eu.tintera.background.tasks.android

import eu.tintera.background.tasks.State
import eu.tintera.background.tasks.core.cleanup.DatabaseCleanupService
import eu.tintera.background.tasks.core.terminalStates
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid

internal class WorkManagerDatabaseCleanupService(
    private val workManagerRepository: WorkManagerRepository,
    private val cleanupServiceRepository: WorkManagerDatabaseCleanupServiceRepository,
    private val clock: Clock,
) : DatabaseCleanupService {

    override suspend fun cleanup() {

        val activeTasks = cleanupServiceRepository.cleanableTasks()

        if (activeTasks.isEmpty()) return

        val workItems = workManagerRepository.find(activeTasks.map { it.id }).associateBy { it.id }

        data class ChangedItem(
            val id: Uuid,
            val state: State,
            val runAttemptCount: Int,
            val finishedAt: Instant
        )

        val itemsWithNewStates = activeTasks.mapNotNull { dbItem ->
            val realInfo = workItems[dbItem.id]
            val state = realInfo?.state ?: State.Cancelled

            if (state != dbItem.state) ChangedItem(
                id = dbItem.id,
                state = state,
                runAttemptCount = realInfo?.runAttemptCount ?: 1,
                finishedAt = realInfo?.let { Instant.DISTANT_PAST } ?: clock.now()
            )
            else null
        }

        itemsWithNewStates.forEach { item ->
            if (item.state in terminalStates) cleanupServiceRepository.terminate(
                taskId = item.id,
                state = item.state,
                finishedAt = item.finishedAt
            ) else cleanupServiceRepository.rewriteState(
                taskId = item.id,
                state = item.state,
                runAttemptCount = item.runAttemptCount
            )
        }
    }
}