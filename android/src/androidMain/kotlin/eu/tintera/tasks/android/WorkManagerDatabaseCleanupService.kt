package eu.tintera.tasks.android

import androidx.work.WorkManager
import androidx.work.WorkQuery
import eu.tintera.tasks.State
import eu.tintera.tasks.core.allowedSourceStatesForChangeTo
import eu.tintera.tasks.core.cleanup.DatabaseCleanupService
import eu.tintera.tasks.core.data.Repository
import eu.tintera.tasks.core.runningStates
import eu.tintera.tasks.core.terminalStates
import kotlinx.coroutines.flow.first
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

internal class WorkManagerDatabaseCleanupService(
    private val repository: Repository,
    private val workManager: WorkManager,
) : DatabaseCleanupService {

    override suspend fun cleanup() {

        val activeTasks = repository.dispatchableTasks(
            states = runningStates
        ).first()

        if (activeTasks.isEmpty()) return

        val workQuery = WorkQuery.Builder.fromIds(activeTasks.map { it.id.toJavaUuid() }).build()
        val realWorkInfos = workManager.getWorkInfosFlow(workQuery).first()
        val realWorkInfoMap = realWorkInfos.associateBy { it.id.toKotlinUuid() }

        data class ChangedItem(
            val id: Uuid,
            val state: State,
            val runAttemptCount: Int,
            val finishedAt: Instant
        )

        val itemsWithNewStates = activeTasks.mapNotNull { dbItem ->
            val realInfo = realWorkInfoMap[dbItem.id]
            val state = realInfo?.state?.toState() ?: State.Cancelled

            if (state != dbItem.state) ChangedItem(
                id = dbItem.id,
                state = state,
                runAttemptCount = realInfo?.runAttemptCount ?: 1,
                finishedAt = realInfo?.let { Instant.DISTANT_PAST } ?: Clock.System.now()
            )
            else null
        }

        itemsWithNewStates.forEach { item ->
            if (item.state in terminalStates) repository.updateTerminatingState(
                id = item.id,
                state = item.state,
                finishedAt = item.finishedAt,
                outputData = null
            ) else repository.updateState(
                id = item.id,
                state = item.state,
                allowedSourceStates = item.state.allowedSourceStatesForChangeTo().toSet(),
                resetProcessTime = true,
                runAttemptCount = item.runAttemptCount
            )
        }
    }
}