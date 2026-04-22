package eu.tintera.tasks.core.cleanup

import eu.tintera.tasks.SimpleTaskHandler
import eu.tintera.tasks.TaskResult
import eu.tintera.tasks.core.data.Repository
import eu.tintera.tasks.core.terminalStates

internal class DatabaseCleanupTaskHandler(
    private val repository: Repository,
    private val cleanupService: DatabaseCleanupService
) : SimpleTaskHandler {

    override suspend fun run(): TaskResult<Unit> {
        cleanupService.cleanup()
        repository.cleanOld(
            terminalStates = terminalStates
        )
        return TaskResult.success(Unit)
    }
}