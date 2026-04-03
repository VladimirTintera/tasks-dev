package eu.tintera.tasks.core

import eu.tintera.tasks.Data
import eu.tintera.tasks.TaskScope
import kotlin.uuid.Uuid

interface TaskScopeFactory {
    fun createScope(
        taskId: Uuid,
        data: Data,
        runAttemptsCount: Int,
    ): TaskScope
}