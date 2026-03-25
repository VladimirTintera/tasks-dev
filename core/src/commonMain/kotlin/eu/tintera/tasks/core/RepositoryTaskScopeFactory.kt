package eu.tintera.tasks.core

import eu.tintera.tasks.Data
import eu.tintera.tasks.ForegroundInfo
import eu.tintera.tasks.TaskScope
import eu.tintera.tasks.core.data.Repository
import kotlin.uuid.Uuid

class RepositoryTaskScopeFactory(
    private val repository: Repository
) : TaskScopeFactory {
    override fun createScope(
        taskId: Uuid,
        data: Data,
        retriesCount: Int
    ): TaskScope = TaskScopeImpl(
        repository = repository,
        taskId = taskId,
        data = data,
        retriesCount = retriesCount
    )

    private class TaskScopeImpl(
        private val repository: Repository,
        override val taskId: Uuid,
        override val data: Data,
        override val retriesCount: Int,
    ) : TaskScope {

        override suspend fun setForegroundInfo(foregroundInfo: ForegroundInfo): Boolean = true

        override suspend fun setProgress(data: Data) {
            repository.updateProgressData(taskId, data)
        }
    }
}