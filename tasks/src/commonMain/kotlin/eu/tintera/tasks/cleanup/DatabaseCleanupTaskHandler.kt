package eu.tintera.tasks.cleanup

import eu.tintera.tasks.TaskHandler
import eu.tintera.tasks.TaskResult
import eu.tintera.tasks.TaskScope
import eu.tintera.tasks.core.data.Repository
import eu.tintera.tasks.core.terminalStates

internal class DatabaseCleanupTaskHandler(
    private val repository: Repository
) : TaskHandler {
    override suspend fun TaskScope.run(): TaskResult {
        repository.cleanOld(terminalStates)
        return TaskResult.success()
    }
}