package eu.tintera.tasks.android

import eu.tintera.tasks.State
import kotlin.time.Instant
import kotlin.uuid.Uuid

interface WorkManagerDatabaseCleanupServiceRepository {
    suspend fun cleanableTasks() : List<TaskWithState>
    suspend fun terminate(
        taskId: Uuid,
        state: State,
        finishedAt: Instant
    )

    suspend fun rewriteState(
        taskId: Uuid,
        state: State,
        runAttemptCount: Int
    )
}