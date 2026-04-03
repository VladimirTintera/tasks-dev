package eu.tintera.tasks.core.fakes

import eu.tintera.tasks.Data
import eu.tintera.tasks.ForegroundInfo
import eu.tintera.tasks.TaskScope
import eu.tintera.tasks.core.TaskScopeFactory
import kotlin.uuid.Uuid

class FakeTaskScopeFactory : TaskScopeFactory {
    override fun createScope(
        taskId: Uuid,
        data: Data,
        runAttemptsCount: Int
    ): TaskScope = object : TaskScope {
        override val taskId: Uuid = taskId
        override var data: Data = data
        override val retryCount: Int = runAttemptsCount

        override suspend fun setForegroundInfo(foregroundInfo: ForegroundInfo): Boolean {
            return true
        }

        override suspend fun setProgress(data: Data) {
            this.data = data
        }

    }
}