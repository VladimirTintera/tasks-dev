package eu.tintera.tasks.core

import eu.tintera.tasks.*
import eu.tintera.tasks.migrations.Migration
import eu.tintera.tasks.serialization.TaskDataSerializer
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration
import kotlin.uuid.Uuid

class TaskManagerImpl(
    private val taskRegistry: TaskRegistry,
    private val coreTaskManager: CoreTaskManager,
) : TaskManager {

    override fun <Input: Any, Output: Any, Progress: Any> register(
        identifier: String,
        currentVersion: Int,
        factory: () -> TaskHandler<Input, Output, Progress>,
        inputSerializer: TaskDataSerializer<Input>,
        outputSerializer: TaskDataSerializer<Output>,
        progressSerializer: TaskDataSerializer<Progress>,
        migrations: List<Migration>
    ) = taskRegistry.register(
        identifier = identifier,
        registration = TaskRegistry.TaskRegistration(
            currentVersion = currentVersion,
            factory = factory,
            inputSerializer = inputSerializer,
            outputSerializer = outputSerializer,
            progressSerializer = progressSerializer,
            migrations = migrations
        )
    )

    override suspend fun <T: Any> enqueueUniqueTask(
        task: TaskRequest<T>,
        uniqueName: String,
        existingTaskPolicy: ExistingTaskPolicy,
    ) = coreTaskManager.enqueueUniqueTask(
        task = task,
        uniqueName = uniqueName,
        existingTaskPolicy = existingTaskPolicy
    )

    override suspend fun <T: Any> enqueueTask(task: TaskRequest<T>) = coreTaskManager.enqueueTask(task)

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

    override suspend fun <T: Any> enqueuePeriodicUniqueTask(
        task: TaskRequest<T>,
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