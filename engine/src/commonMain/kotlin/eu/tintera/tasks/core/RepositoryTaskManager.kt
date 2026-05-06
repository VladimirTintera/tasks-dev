package eu.tintera.tasks.core

import eu.tintera.tasks.*
import eu.tintera.tasks.core.data.*
import eu.tintera.tasks.core.migrations.TaskMigrator
import kotlinx.coroutines.flow.*
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.uuid.Uuid

class RepositoryCoreTaskManager(
    private val repository: Repository,
    private val taskRegistry: RegistryResolver,
    private val taskMigrator: TaskMigrator,
    private val tagMapper: TagMapper,
    private val transactionRunner: TransactionRunner
) : TaskManager {

    override suspend fun <T : Any> enqueueUniqueTask(
        task: TaskRequest<T>,
        uniqueName: String,
        existingTaskPolicy: ExistingTaskPolicy,
    ): Uuid = transactionRunner {

        val existing = allUndoneByUniqueName(uniqueName)

        val parentIds: List<Uuid> = when (existingTaskPolicy) {
            ExistingTaskPolicy.Keep -> {
                if (existing.isNotEmpty())
                    return@transactionRunner existing.first().id
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
            registration = findRegistration<T, Any, Any>(task.identifier),
            repeatInterval = null
        )

        repository.insert(t, task.serializeTags() + uniqueName + t.identifier, parentIds.toSet())

        t.id
    }

    private suspend fun TaskRequest<*>.serializeTags() = tagMapper.serialize(tags)

    private suspend fun <I : Any, O : Any, P : Any> findRegistrationOrNull(
        identifier: String
    ) = taskRegistry.resolve<I, O, P>(identifier)

    private suspend fun <I : Any, O : Any, P : Any> findRegistration(
        identifier: String
    ) = findRegistrationOrNull<I, O, P>(identifier) ?: error("Task '$identifier' is not registered!")


    override suspend fun enqueueContinuation(
        continuation: TaskContinuation,
    ) = transactionRunner {
        val uniqueName = Uuid.random().toString()
        repository.insertContinuation(
            taskContinuation = continuation,
            uniqueName = uniqueName,
            parentIds = setOf()
        )
    }

    private suspend fun allUndoneByUniqueName(
        uniqueName: String
    ) = repository.allByUniqueName(uniqueName).let { list ->
        list.filter { !it.state.terminal() }
    }

    override suspend fun enqueueUniqueContinuation(
        continuation: TaskContinuation,
        uniqueName: String,
        existingTaskPolicy: ExistingTaskPolicy,
    ) = transactionRunner {

        val existing = allUndoneByUniqueName(uniqueName)

        val parentIds: List<Uuid> = when (existingTaskPolicy) {
            ExistingTaskPolicy.Keep -> {
                if (existing.any { !it.state.terminal() })
                    return@transactionRunner
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

        repository.insertContinuation(continuation, uniqueName, parentIds.toSet())
    }

    private fun <T : Any> TaskRequest<T>.toTask(
        uniqueName: String,
        state: State,
        registration: TaskRegistration<T, *, *>,
        repeatInterval: Duration?
    ): Task {
        val now = Clock.System.now()
        return Task(
            id = Uuid.random(),
            identifier = identifier,
            uniqueName = uniqueName,
            runAttemptCount = 0,
            state = state,
            processTime = initialDelay.takeIf { it.isPositive() }?.let {
                now + it
            },
            inputData = registration.inputSerializer.encodeToBytes(data),
            outputData = null,
            networkRequired = constraints.requiresNetwork,
            createdAt = now,
            finishedAt = null,
            repeatInterval = repeatInterval,
            initialDelay = initialDelay,
            backoffCriteria = backoffCriteria ?: BackoffCriteria.DEFAULT,
            progressData = null,
            retentionDelay = keepResultsForAtLeast,
            requiresDeviceIdle = constraints.requiresDeviceIdle,
            version = registration.currentVersion
        )
    }

    override suspend fun <T : Any> enqueueTask(
        task: TaskRequest<T>,
    ): Uuid = transactionRunner {
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

    private suspend fun <T : Any> insertTask(
        taskRequest: TaskRequest<T>,
        uniqueName: String,
        parentIds: Set<Uuid>,
    ): Uuid {
        val task = taskRequest.toTask(
            uniqueName = uniqueName,
            state = if (parentIds.isEmpty()) State.Enqueued else State.Blocked,
            registration = findRegistration<T, Any, Any>(taskRequest.identifier),
            repeatInterval = null
        )
        repository.insert(task, taskRequest.serializeTags() + taskRequest.identifier, parentIds)
        return task.id
    }

    override suspend fun <T : Any> enqueuePeriodicUniqueTask(
        task: TaskRequest<T>,
        repeatInterval: Duration,
        uniqueName: String,
        existingTaskPolicy: ExistingPeriodicTaskPolicy,
    ): Uuid = transactionRunner {

        val existing = repository.allByUniqueName(uniqueName)

        when (existingTaskPolicy) {
            ExistingPeriodicTaskPolicy.Keep -> {
                val notCompleted = existing.firstOrNull { !it.state.terminal() }
                if (notCompleted != null)
                    return@transactionRunner notCompleted.id
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
            registration = findRegistration<T, Any, Any>(task.identifier),
            repeatInterval = repeatInterval.coerceAtLeast(MINIMAL_REPEAT_INTERVAL)
        )

        repository.insert(t, task.serializeTags() + uniqueName + t.identifier, emptySet())
        t.id
    }

    override fun taskInfosByTag(
        tag: String,
    ): Flow<List<TaskInfo>> = repository.taskInfosByTag(tag).distinctUntilChanged().map { map ->
        map.map { it.toTaskInfo() }
    }

    override fun taskInfos(query: TaskInfoQuery) = query.takeIf { !it.isEmpty() }?.let {
        flow {
            val tags = tagMapper.serialize(query.tags)

            repository.taskInfos(
                tags = tags,
                ids = query.ids,
                states = query.states,
                uniqueNames = query.uniqueNames,
            ).distinctUntilChanged().map { list ->
                list.map { it.toTaskInfo() }
            }.collect {
                emit(it)
            }
        }

    } ?: emptyFlow()

    override fun taskInfoById(
        id: Uuid,
    ): Flow<TaskInfo?> = repository.taskInfoById(id).distinctUntilChanged().map { info ->
        info?.toTaskInfo()
    }

    private suspend fun Info.toTaskInfo(): TaskInfo {
        val registration = findRegistrationOrNull<Any, Any, Any>(identifier)
        return toTaskInfo(
            registration = registration,
            migrationResult = registration?.let {
                taskMigrator.migrate(
                    data = this,
                    registration = registration
                )
            },
            tags = tagMapper.parse(tags)
        )
    }

    override suspend fun cancelTaskById(id: Uuid) = transactionRunner {
        cancelTask(id)
    }

    override suspend fun cancelTasksByTag(
        tag: String
    ) = transactionRunner {

        val tasks = repository.taskIdsByTagAndState(
            tag = tag,
            states = listOf(State.Running, State.Blocked, State.Enqueued)
        )

        tasks.forEach {
            cancelTask(it)
        }
    }

    private suspend fun cancelTask(taskId: Uuid) {
        repository.finishTaskWithUnsuccess(
            id = taskId,
            state = State.Cancelled,
            finishedAt = Clock.System.now()
        )
    }
}