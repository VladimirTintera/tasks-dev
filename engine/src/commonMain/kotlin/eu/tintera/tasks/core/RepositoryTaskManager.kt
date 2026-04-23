package eu.tintera.tasks.core

import eu.tintera.tasks.*
import eu.tintera.tasks.core.data.Repository
import eu.tintera.tasks.core.data.Task
import eu.tintera.tasks.core.data.toTaskInfo
import eu.tintera.tasks.core.migrations.TaskMigrator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.uuid.Uuid

class RepositoryCoreTaskManager(
    private val repository: Repository,
    private val taskRegistry: RegistryResolver,
    private val taskMigrator: TaskMigrator
) : TaskManager {

    override suspend fun <T : Any> enqueueUniqueTask(
        task: TaskRequest<T>,
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
            registration = findRegistration<T, Any, Any>(task.identifier),
            repeatInterval = null
        )

        insert(t, task.tags + uniqueName + t.identifier, parentIds.toSet())

        t.id
    }

    private suspend fun <I : Any, O : Any, P : Any> findRegistrationOrNull(
        identifier: String
    ) = taskRegistry.resolve<I, O, P>(identifier)

    private suspend fun <I : Any, O : Any, P : Any> findRegistration(
        identifier: String
    ) = findRegistrationOrNull<I, O, P>(identifier) ?: error("Task '$identifier' is not registered!")


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

    private suspend fun <T : Any> Repository.insertTask(
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
        insert(task, taskRequest.tags + taskRequest.identifier, parentIds)
        return task.id
    }

    override suspend fun <T : Any> enqueuePeriodicUniqueTask(
        task: TaskRequest<T>,
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
            registration = findRegistration<T, Any, Any>(task.identifier),
            repeatInterval = repeatInterval.coerceAtLeast(MINIMAL_REPEAT_INTERVAL)
        )

        insert(t, task.tags + uniqueName + t.identifier, emptySet())
        t.id
    }

    override fun taskInfosByTag(
        tag: String,
    ): Flow<List<TaskInfo>> = repository.taskInfosByTag(tag).distinctUntilChanged().map { map ->
        map.map { info ->
            val registration = findRegistrationOrNull<Any, Any, Any>(info.identifier)
            info.toTaskInfo(
                registration = registration,
                migrationResult = registration?.let {
                    taskMigrator.migrate(
                        data = info,
                        registration = registration
                    )
                }
            )
        }
    }

    override fun taskInfoById(
        id: Uuid,
    ): Flow<TaskInfo?> = repository.taskInfoById(id).distinctUntilChanged().map { info ->
        info?.let {
            val registration = findRegistrationOrNull<Any, Any, Any>(info.identifier)
            info.toTaskInfo(
                registration = registration,
                migrationResult = registration?.let {
                    taskMigrator.migrate(
                        data = info,
                        registration = registration
                    )
                }
            )
        }
    }

    override suspend fun cancelTaskById(id: Uuid) = repository.withTransaction {
        cancelTask(id)
    }

    override suspend fun cancelTasksByTag(
        tag: String
    ) = repository.withTransaction {

        val tasks = taskIdsByTagAndState(
            tag = tag,
            states = listOf(State.Running, State.Blocked, State.Enqueued)
        )

        tasks.forEach {
            cancelTask(it)
        }
    }

    private suspend fun Repository.cancelTask(taskId: Uuid) {
        updateTerminatingStateWithDescendants(
            id = taskId,
            state = State.Cancelled,
            allowedSourceStates = State.Cancelled.allowedSourceStatesForChangeTo().toSet(),
            finishedAt = Clock.System.now()
        )
    }
}