package eu.tintera.tasks.core

import eu.tintera.tasks.*
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration
import kotlin.uuid.Uuid

class TaskManagerImpl(
    private val taskRegistry: TaskRegistry,
    private val coreTaskManager: CoreTaskManager,
) : TaskManager {
    override fun register(
        identifier: String,
        factory: () -> TaskHandler,
    ) = taskRegistry.register(identifier, factory)


    override suspend fun enqueueUniqueTask(
        task: TaskRequest,
        uniqueName: String,
        existingTaskPolicy: ExistingTaskPolicy,
    ) = coreTaskManager.enqueueUniqueTask(
        task = task,
        uniqueName = uniqueName,
        existingTaskPolicy = existingTaskPolicy
    )

    override suspend fun enqueueTask(task: TaskRequest) = coreTaskManager.enqueueTask(task)

    override suspend fun enqueueContinuation(
        continuation: TaskContinuation,
    ) = coreTaskManager.enqueueContinuation(continuation)

    override suspend fun enqueueUniqueContinuation(
        continuation: TaskContinuation,
        uniqueName: String,
        existingTaskPolicy: ExistingTaskPolicy,
    ) = coreTaskManager.enqueueUniqueContinuation(
        continuation = continuation,
        uniqueName = uniqueName,
        existingTaskPolicy = existingTaskPolicy
    )

    override suspend fun enqueuePeriodicUniqueTask(
        task: TaskRequest,
        repeatInterval: Duration,
        uniqueName: String,
        existingTaskPolicy: ExistingPeriodicTaskPolicy,
    ) = coreTaskManager.enqueuePeriodicUniqueTask(
        task = task,
        repeatInterval = repeatInterval,
        uniqueName = uniqueName,
        existingTaskPolicy = existingTaskPolicy
    )

    override fun taskInfoById(id: Uuid): Flow<TaskInfo?> = coreTaskManager.taskInfoById(id)

    override fun taskInfosByTag(tag: String) = coreTaskManager.taskInfosByTag(tag)

    override suspend fun cancelTaskById(id: Uuid) = coreTaskManager.cancelTaskById(id)

    override suspend fun cancelTasksByTag(tag: String) = coreTaskManager.cancelTasksByTag(tag)
}