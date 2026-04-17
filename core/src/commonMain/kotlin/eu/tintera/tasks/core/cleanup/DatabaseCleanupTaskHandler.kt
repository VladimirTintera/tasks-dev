package eu.tintera.tasks.core.cleanup

import eu.tintera.tasks.TaskHandler
import eu.tintera.tasks.TaskResult
import eu.tintera.tasks.TaskScope
import eu.tintera.tasks.core.data.Repository
import eu.tintera.tasks.core.terminalStates

internal class DatabaseCleanupTaskHandler(
    private val repository: Repository
) : TaskHandler<Unit, Unit, Unit> {
    override suspend fun TaskScope<Unit, Unit>.run(): TaskResult<Unit> {
        // todo odstranit nevalidne zaplanovane requesty z workmanagera state == Enqueued AND runAttemptCount == 0 AND (now > createdAt + initialDelay + 30.days)
        repository.cleanOld(terminalStates)
        return TaskResult.success(Unit)
    }
}