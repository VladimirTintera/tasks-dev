package eu.tintera.tasks.core

import eu.tintera.tasks.*
import eu.tintera.tasks.core.data.FullTask
import eu.tintera.tasks.core.data.Repository
import eu.tintera.tasks.core.data.Task
import eu.tintera.tasks.core.seriaization.SerializationEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.KSerializer
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.uuid.Uuid

class RepositoryCoreTaskManager(
    private val repository: Repository,
    private val taskRegistry: TaskRegistry,
    private val serializationEngine: SerializationEngine
) : CoreTaskManager {

    override suspend fun enqueueUniqueTask(
        task: TaskRequest<*>,
        uniqueName: String,
        existingTaskPolicy: ExistingTaskPolicy,
    ): Uuid = repository.withTransaction {

        val existing = allUndoneByUniqueName(uniqueName)

        val parentIds: List<Uuid> = when (existingTaskPolicy) {
            ExistingTaskPolicy.Keep -> {
                if (existing.isNotEmpty())
                    return@withTransaction existing.first().id
                emptyList()
            }

            ExistingTaskPolicy.Replace -> {
                existing.forEach {
                    cancelTask(it.id)
                }
                emptyList()
            }

            ExistingTaskPolicy.Append -> existing.map { it.id }
        }

        val t = task.toTask(
            uniqueName = uniqueName,
            state = if (parentIds.isEmpty()) State.Enqueued else State.Blocked,
            registration = findRegistration(task.identifier),
            repeatInterval = null
        )

        insert(t, task.tags + uniqueName + t.identifier, parentIds.toSet())

        t.id
    }

    private suspend fun findRegistrationOrNull(
        identifier: String
    ) = taskRegistry.resolve(identifier)

    private suspend fun findRegistration(
        identifier: String
    ) = findRegistrationOrNull(identifier) ?: error("Task '$identifier' is not registered!")


    override suspend fun enqueueContinuation(
        continuation: TaskContinuation,
    ) = repository.withTransaction {
        val uniqueName = Uuid.random().toString()
        insertContinuation(
            taskContinuation = continuation,
            uniqueName = uniqueName,
            parentIds = setOf()
        )
    }

    private suspend fun Repository.allUndoneByUniqueName(
        uniqueName: String
    ) = allByUniqueName(uniqueName).let { list ->
        list.filter { !it.state.terminal() }
    }

    override suspend fun enqueueUniqueContinuation(
        continuation: TaskContinuation,
        uniqueName: String,
        existingTaskPolicy: ExistingTaskPolicy,
    ) = repository.withTransaction {

        val existing = allUndoneByUniqueName(uniqueName)

        val parentIds: List<Uuid> = when (existingTaskPolicy) {
            ExistingTaskPolicy.Keep -> {
                if (existing.any { !it.state.terminal() })
                    return@withTransaction
                emptyList()
            }

            ExistingTaskPolicy.Replace -> {
                existing.forEach {
                    cancelTask(it.id)
                }
                emptyList()
            }

            ExistingTaskPolicy.Append -> existing.map { it.id }
        }

        insertContinuation(continuation, uniqueName, parentIds.toSet())
    }

    private fun TaskRequest<*>.toTask(
        uniqueName: String,
        state: State,
        registration: TaskRegistry.TaskRegistration<*, *, *>,
        repeatInterval: Duration?
    ) = Task(
        id = Uuid.random(),
        identifier = identifier,
        uniqueName = uniqueName,
        runAttemptCount = 0,
        state = state,
        processTime = Clock.System.now(),
        inputData = data?.let {
            serializationEngine.encodeToBytes(
                value = data,
                serializer = registration.inputSerializer as KSerializer<Any?>
            )
        },
        outputData = null,
        networkRequired = constraints.requiresNetwork,
        createdAt = Clock.System.now(),
        finishedAt = null,
        repeatInterval = repeatInterval,
        initialDelay = initialDelay,
        backoffCriteria = backoffCriteria ?: BackoffCriteria.DEFAULT,
        progressData = null,
        retentionDelay = keepResultsForAtLeast,
        requiresDeviceIdle = constraints.requiresDeviceIdle,
        version = registration.currentVersion
    )

    override suspend fun enqueueTask(
        task: TaskRequest<*>,
    ): Uuid = repository.withTransaction {
        insertTask(task, Uuid.random().toString(), setOf())
    }


    private suspend fun Repository.insertContinuation(
        taskContinuation: TaskContinuation,
        uniqueName: String,
        parentIds: Set<Uuid>,
    ) {
        val ids = taskContinuation.tasks.map {
            insertTask(it, uniqueName, parentIds)
        }.toSet()
        taskContinuation.next?.also {
            insertContinuation(it, uniqueName, ids)
        }
    }

    private suspend fun Repository.insertTask(
        taskRequest: TaskRequest<*>,
        uniqueName: String,
        parentIds: Set<Uuid>,
    ): Uuid {
        val task = taskRequest.toTask(
            uniqueName = uniqueName,
            state = if (parentIds.isEmpty()) State.Enqueued else State.Blocked,
            registration = findRegistration(taskRequest.identifier),
            repeatInterval = null
        )
        insert(task, taskRequest.tags + taskRequest.identifier, parentIds)
        return task.id
    }

    override suspend fun enqueuePeriodicUniqueTask(
        task: TaskRequest<*>,
        repeatInterval: Duration,
        uniqueName: String,
        existingTaskPolicy: ExistingPeriodicTaskPolicy,
    ): Uuid = repository.withTransaction {

        val existing = allByUniqueName(uniqueName)

        when (existingTaskPolicy) {
            ExistingPeriodicTaskPolicy.Keep -> {
                val notCompleted = existing.firstOrNull { !it.state.terminal() }
                if (notCompleted != null)
                    return@withTransaction notCompleted.id
            }

            ExistingPeriodicTaskPolicy.Replace -> {
                existing.forEach {
                    cancelTask(it.id)
                }
            }
        }

        val t = task.toTask(
            uniqueName = uniqueName,
            state = State.Enqueued,
            registration = findRegistration(task.identifier),
            repeatInterval = repeatInterval.coerceAtLeast(MINIMAL_REPEAT_INTERVAL)
        )

        insert(t, task.tags + uniqueName + t.identifier, emptySet())
        t.id
    }

    override fun taskInfosByTag(
        tag: String,
    ): Flow<List<TaskInfo>> = repository.tasksByTag(tag).distinctUntilChanged().map { map ->
        map.map { it.toTaskInfo() }
    }

    override fun taskInfoById(
        id: Uuid,
    ): Flow<TaskInfo?> = repository.taskById(id).distinctUntilChanged().map { task ->
        task?.toTaskInfo()
    }

    private suspend fun FullTask.toTaskInfo(): TaskInfo {
        val registration = findRegistrationOrNull(task.identifier)
        return TaskInfo(
            id = task.id,
            runAttemptCount = task.runAttemptCount,
            state = task.state,
            tags = tags,
            outputData = task.outputData?.let { byteArray ->
                registration?.outputSerializer?.let {
                    byteArray.toTypedData(
                        serializationEngine = serializationEngine,
                        serializer = it
                    )
                }
            },
            nextScheduledTime = task.processTime,
            progress = task.progressData?.let { byteArray ->
                registration?.progressSerializer?.let {
                    byteArray.toTypedData(
                        serializationEngine = serializationEngine,
                        serializer = it
                    )
                }
            },
            finishedAt = task.finishedAt,
            createdAt = task.createdAt
        )
    }

    override suspend fun cancelTaskById(id: Uuid) = repository.withTransaction {
        cancelTask(id)
    }

    override suspend fun cancelTasksByTag(
        tag: String
    ) = repository.withTransaction {

        val tasks = tasksByTagAndState(
            tag = tag,
            states = listOf(State.Running, State.Blocked, State.Enqueued)
        )

        tasks.forEach {
            cancelTask(it.id)
        }
    }

    private suspend fun Repository.cancelTask(taskId: Uuid) {
        updateStateWithDescendants(
            id = taskId,
            state = State.Cancelled,
            allowedSourceStates = State.Cancelled.allowedSourceStatesForChangeTo().toSet()
        )
    }
}