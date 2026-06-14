package eu.tintera.background.tasks.core.cleanup

import eu.tintera.background.tasks.SimpleTaskHandler
import eu.tintera.background.tasks.TaskResult
import eu.tintera.background.tasks.core.data.Repository
import eu.tintera.background.tasks.core.terminalStates

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