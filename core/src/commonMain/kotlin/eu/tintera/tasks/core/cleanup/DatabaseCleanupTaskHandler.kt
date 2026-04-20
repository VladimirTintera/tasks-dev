package eu.tintera.tasks.core.cleanup

import eu.tintera.tasks.SimpleTaskHandler
import eu.tintera.tasks.TaskResult
import eu.tintera.tasks.core.data.Repository
import eu.tintera.tasks.core.terminalStates

internal class DatabaseCleanupTaskHandler(
    private val repository: Repository,
    private val cleanupPolicy: DatabaseCleanupPolicy
) : SimpleTaskHandler {

    override suspend fun run(): TaskResult<Unit> {
        // TODO: odstranit nevalidne zaplanovane requesty z workmanagera state == Enqueued AND runAttemptCount == 0 AND (now > createdAt + initialDelay + 30.days)
        repository.cleanOld(
            terminalStates = terminalStates
        )
        return TaskResult.success(Unit)
    }
}