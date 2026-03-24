package eu.tintera.tasks.core

import eu.tintera.tasks.ExistingPeriodicTaskPolicy
import eu.tintera.tasks.ExistingTaskPolicy
import eu.tintera.tasks.TaskContinuation
import eu.tintera.tasks.TaskInfo
import eu.tintera.tasks.TaskRequest
import eu.tintera.tasks.fullName
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration
import kotlin.uuid.Uuid

interface CoreTaskManager {
    suspend fun enqueueUniqueTask(
        task: TaskRequest,
        uniqueName: String = task.handler.fullName,
        existingTaskPolicy: ExistingTaskPolicy = ExistingTaskPolicy.Keep
    ) : Uuid

    suspend fun enqueueTask(
        task: TaskRequest
    ) : Uuid

    suspend fun enqueueContinuation(
        continuation: TaskContinuation
    )

    suspend fun enqueueUniqueContinuation(
        continuation: TaskContinuation,
        uniqueName: String,
        existingTaskPolicy: ExistingTaskPolicy = ExistingTaskPolicy.Keep
    )

    suspend fun enqueuePeriodicUniqueTask(
        task: TaskRequest,
        repeatInterval: Duration,
        uniqueName: String = task.handler.fullName,
        existingTaskPolicy: ExistingPeriodicTaskPolicy = ExistingPeriodicTaskPolicy.Keep
    ) : Uuid

    fun taskInfoById(id: Uuid): Flow<TaskInfo?>
    fun taskInfosByTag(tag: String): Flow<List<TaskInfo>>
    suspend fun cancelTaskById(id: Uuid)
    suspend fun cancelTasksByTag(tag: String)
}