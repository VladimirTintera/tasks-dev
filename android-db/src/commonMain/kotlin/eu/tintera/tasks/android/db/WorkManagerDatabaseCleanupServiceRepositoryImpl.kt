package eu.tintera.tasks.android.db

import eu.tintera.tasks.State
import eu.tintera.tasks.android.TaskWithState
import eu.tintera.tasks.android.WorkManagerDatabaseCleanupServiceRepository
import eu.tintera.tasks.core.runningStates
import eu.tintera.tasks.db.dao.CleanableTaskDao
import eu.tintera.tasks.db.toEntityState
import eu.tintera.tasks.db.toTaskState
import kotlin.time.Instant
import kotlin.uuid.Uuid

internal class WorkManagerDatabaseCleanupServiceRepositoryImpl(
    private val cleanableTaskDao: CleanableTaskDao
) : WorkManagerDatabaseCleanupServiceRepository {

    override suspend fun cleanableTasks(): List<TaskWithState> = cleanableTaskDao.tasksByStates(
        states = runningStates.map { it.toEntityState() }
    ).map {
        TaskWithState(
            id = it.id,
            state = it.state.toTaskState()
        )
    }

    override suspend fun terminate(
        taskId: Uuid,
        state: State,
        finishedAt: Instant
    ) {
        cleanableTaskDao.terminateTask(
            id = taskId,
            state = state.toEntityState(),
            finishedAt = finishedAt
        )
    }

    override suspend fun rewriteState(
        taskId: Uuid,
        state: State,
        runAttemptCount: Int
    ) {
        cleanableTaskDao.rewriteTaskState(
            id = taskId,
            state = state.toEntityState(),
            runAttemptCount = runAttemptCount
        )
    }
}