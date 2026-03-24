package eu.tintera.tasks.core

import eu.tintera.tasks.*
import eu.tintera.tasks.core.data.Repository
import eu.tintera.tasks.core.data.Task
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.uuid.Uuid

class RepositoryCoreTaskManager(
    private val repository: Repository,
) : CoreTaskManager {

    override suspend fun enqueueUniqueTask(
        task: TaskRequest,
        uniqueName: String,
        existingTaskPolicy: ExistingTaskPolicy,
    ): Uuid = repository.withTransaction {
        cleanOld()

        val existing = allUndoneByUniqueName(uniqueName)

        val parentIds: List<Uuid> = when (existingTaskPolicy) {
            ExistingTaskPolicy.Keep -> {
                if (existing.isNotEmpty())
                    return@withTransaction existing.first().id
                existing.map { it.id }
            }

            ExistingTaskPolicy.Replace -> {
                existing.forEach {
                    delete(it.id)
                }
                listOf()
            }

            ExistingTaskPolicy.Append -> existing.map { it.id }
        }

        val t = Task(
            id = Uuid.random(),
            identifier = task.handler.fullName,
            uniqueName = uniqueName,
            retriesCount = 0,
            state = if (parentIds.isEmpty()) State.Enqueued else State.Blocked,
            processTime = Clock.System.now(),
            inputData = task.data,
            outputData = Data.EMPTY,
            networkRequired = task.constraints.requiresNetwork,
            createdAt = Clock.System.now(),
            finishedAt = null,
            repeatInterval = null,
            initialDelay = task.initialDelay,
            backoffCriteria = task.backoffCriteria ?: BackoffCriteria.DEFAULT,
            progressData = Data.EMPTY,
            retentionDelay = task.keepResultsForAtLeast
        )

        insert(t, task.tags + uniqueName + t.identifier, parentIds.toSet())

        t.id
    }


    override suspend fun enqueueContinuation(
        continuation: TaskContinuation,
    ) = repository.withTransaction {
        cleanOld()
        val uniqueName = Uuid.random().toString()
        insertContinuation(
            taskContinuation = continuation,
            uniqueName = uniqueName,
            parentIds = setOf()
        )
    }

    private suspend fun Repository.allUndoneByUniqueName(
        uniqueName: String
    ) = repository.allByUniqueName(uniqueName).let { list ->
        list.forEach {
            if (it.state.terminal())
                delete(it.id)
        }
        list.filter { !it.state.terminal() }
    }

    override suspend fun enqueueUniqueContinuation(
        continuation: TaskContinuation,
        uniqueName: String,
        existingTaskPolicy: ExistingTaskPolicy,
    ) = repository.withTransaction {
        cleanOld()

        val existing = allUndoneByUniqueName(uniqueName)

        val parentIds: List<Uuid> = when (existingTaskPolicy) {
            ExistingTaskPolicy.Keep -> {
                if (existing.any { !it.state.terminal() })
                    return@withTransaction
                existing.forEach {
                    delete(it.id)
                }
                listOf()
            }

            ExistingTaskPolicy.Replace -> {
                existing.forEach {
                    delete(it.id)
                }
                listOf()
            }

            ExistingTaskPolicy.Append -> existing.map { it.id }
        }

        insertContinuation(continuation, uniqueName, parentIds.toSet())
    }

    private fun TaskRequest.toTask(
        uniqueName: String,
        state: State,
    ) = Task(
        id = Uuid.random(),
        identifier = handler.fullName,
        uniqueName = uniqueName,
        retriesCount = 0,
        state = state,
        processTime = Clock.System.now(),
        inputData = data,
        outputData = Data.EMPTY,
        networkRequired = constraints.requiresNetwork,
        createdAt = Clock.System.now(),
        finishedAt = null,
        repeatInterval = null,
        initialDelay = initialDelay,
        backoffCriteria = backoffCriteria ?: BackoffCriteria.DEFAULT,
        progressData = Data.EMPTY,
        retentionDelay = keepResultsForAtLeast
    )

    override suspend fun enqueueTask(
        task: TaskRequest,
    ): Uuid = repository.withTransaction {
        cleanOld()
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
        taskRequest: TaskRequest,
        uniqueName: String,
        parentIds: Set<Uuid>,
    ): Uuid {
        val task = taskRequest.toTask(
            uniqueName,
            if (parentIds.isEmpty()) State.Enqueued else State.Blocked
        )
        insert(task, taskRequest.tags + taskRequest.handler.fullName, parentIds)
        return task.id
    }

    override suspend fun enqueuePeriodicUniqueTask(
        task: TaskRequest,
        repeatInterval: Duration,
        uniqueName: String,
        existingTaskPolicy: ExistingPeriodicTaskPolicy,
    ): Uuid = repository.withTransaction {

        cleanOld()

        val existing = allByUniqueName(uniqueName)

        when (existingTaskPolicy) {
            ExistingPeriodicTaskPolicy.Keep -> {
                val notCompleted = existing.firstOrNull { !it.state.terminal() }
                if (notCompleted != null)
                    return@withTransaction notCompleted.id
                existing.forEach {
                    delete(it.id)
                }
            }

            ExistingPeriodicTaskPolicy.Replace -> {
                existing.forEach {
                    delete(it.id)
                }
            }
        }

        val t = Task(
            id = Uuid.random(),
            identifier = task.handler.fullName,
            uniqueName = uniqueName,
            retriesCount = 0,
            state = State.Enqueued,
            processTime = Clock.System.now(),
            inputData = task.data,
            outputData = Data.EMPTY,
            networkRequired = task.constraints.requiresNetwork,
            createdAt = Clock.System.now(),
            finishedAt = null,
            repeatInterval = repeatInterval.coerceAtLeast(MINIMAL_REPEAT_INTERVAL),
            initialDelay = task.initialDelay,
            backoffCriteria = task.backoffCriteria ?: BackoffCriteria.DEFAULT,
            progressData = Data.EMPTY,
            retentionDelay = task.keepResultsForAtLeast
        )

        insert(t, task.tags + uniqueName + t.identifier, emptySet())
        t.id
    }

    override fun taskInfosByTag(
        tag: String,
    ): Flow<List<TaskInfo>> = repository.tasksByTag(tag).distinctUntilChanged().map { map ->
        map.map {
            TaskInfo(
                id = it.task.id,
                retriesCount = it.task.retriesCount,
                state = it.task.state,
                tags = it.tags,
                outputData = it.task.outputData,
                nextScheduledTime = it.task.processTime,
                progress = it.task.progressData ?: Data.EMPTY
            )
        }
    }

    override fun taskInfoById(
        id: Uuid,
    ): Flow<TaskInfo?> = repository.taskById(id).distinctUntilChanged().map { task ->
        task?.let {
            TaskInfo(
                id = it.task.id,
                retriesCount = it.task.retriesCount,
                state = it.task.state,
                tags = it.tags,
                outputData = it.task.outputData,
                nextScheduledTime = it.task.processTime,
                progress = it.task.progressData ?: Data.EMPTY
            )
        }
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

    private suspend fun Repository.cleanOld() = cleanOld(
        states = terminalStates
    )

    private suspend fun Repository.cancelTask(taskId: Uuid) {
        updateStateWithDescendants(
            id = taskId,
            state = State.Cancelled,
            allowedSourceStates = State.Cancelled.allowedSourceStatesForChangeTo().toSet()
        )
    }
}