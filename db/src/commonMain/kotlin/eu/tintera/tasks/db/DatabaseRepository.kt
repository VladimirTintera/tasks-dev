package eu.tintera.tasks.db

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import eu.tintera.tasks.State
import eu.tintera.tasks.TaskInfoQuery
import eu.tintera.tasks.core.data.*
import eu.tintera.tasks.db.entities.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid

internal class DatabaseRepository(
    private val db: TasksDatabase,
    private val taskDao: TaskDao,
    private val taskTagDao: TaskTagDao,
    private val taskParentTaskDao: TaskParentTaskDao
) : Repository {
    override fun dispatchableTasks(states: List<State>): Flow<List<DispatchableTask>> =
        taskDao.getDispatchableTasksByStates(
            states = states.map { it.toEntityState() }
        ).distinctUntilChanged().map {
            it.map { task ->
                DispatchableTask(
                    id = task.id,
                    state = task.state.toTaskState()
                )
            }
        }

    override fun processableTask(
        id: Uuid
    ) = taskDao.processableTask(id).distinctUntilChanged().map {
        it?.let {
            ProcessableTask(
                id = id,
                state = it.state.toTaskState(),
                initialDelay = it.initialDelay,
                runAttemptCount = it.runAttemptCount,
                networkRequired = it.networkRequired,
                requiresDeviceIdle = it.requiresDeviceIdle,
                repeatInterval = it.repeatInterval,
                backoffCriteria = it.backoffCriteria?.toTaskBackoffCriteria(),
                processTime = it.processTime
            )
        }
    }

    override suspend fun executableTask(id: Uuid): ExecutableTask? =
        taskDao.getExecutableTasksById(id)?.map { (task, tags) ->
            ExecutableTask(
                identifier = task.identifier,
                runAttemptCount = task.runAttemptCount,
                version = task.version,
                inputData = task.inputData,
                outputData = task.outputData,
                progressData = task.progressData,
                tags = tags.map { it.name }.toSet()
            )
        }?.firstOrNull()


    override suspend fun parentsDataFor(id: Uuid) = taskDao.parentsDataFor(id).map {
        ParentData(
            id = it.id,
            identifier = it.identifier,
            outputData = it.outputData,
            finishedAt = it.finishedAt ?: Instant.DISTANT_PAST,
            version = it.version
        )
    }

    override fun parentStatesForTask(id: Uuid) = taskDao.parentStatesForTask(id).map { list ->
        list.map { it.toTaskState() }
    }.distinctUntilChanged()

    override suspend fun updateNextRun(
        id: Uuid,
        processTime: Instant,
        state: State,
        progressData: ByteArray?,
        runAttemptCount: Int?
    ) = taskDao.updateRetry(
        id = id,
        processTime = processTime,
        state = state.toEntityState(),
        progress = progressData,
        runAttemptCount = runAttemptCount
    )

    override suspend fun updateRunAttemptCount(
        id: Uuid,
        runAttemptsCount: Int
    ) = taskDao.updateRunAttemptCount(
        id = id,
        runAttemptCount = runAttemptsCount
    )

    override suspend fun updateTerminatingState(
        id: Uuid,
        state: State,
        finishedAt: Instant,
        outputData: ByteArray?
    ) = taskDao.updateTerminatingState(
        id = id,
        state = state.toEntityState(),
        finishedAt = finishedAt,
        outputData = outputData
    )

    override suspend fun task(
        id: Uuid
    ) = taskDao.task(id)?.toTask()

    override suspend fun allByUniqueName(
        uniqueName: String
    ) = taskDao.allByUniqueName(uniqueName).map { it.toTask() }

    override suspend fun delete(id: Uuid) = taskDao.delete(id)
    override suspend fun insert(
        task: Task,
        tags: Set<String>,
        parentIds: Set<Uuid>
    ) = withTransaction {
        taskDao.insert(
            task.toTaskEntity()
        )

        if (tags.isNotEmpty()) taskTagDao.insert(
            tags.map {
                TaskTag(
                    taskId = task.id,
                    name = it
                )
            }
        )

        parentIds.forEach {
            taskParentTaskDao.insert(
                TaskParentTask(
                    taskId = task.id,
                    parentTaskId = it
                )
            )
        }

    }

    override suspend fun cleanOld(
        terminalStates: Set<State>
    ) = taskDao.cleanOld(Clock.System.now().toEpochMilliseconds(), terminalStates.map { it.toEntityState() })

    private fun InfoEntity.toInfo(tags: Set<String>) = Info(
        id = id,
        identifier = identifier,
        runAttemptCount = runAttemptCount,
        state = state.toTaskState(),
        tags = tags,
        outputData = outputData,
        processTime = processTime,
        progressData = progressData,
        finishedAt = finishedAt,
        createdAt = createdAt ?: Instant.DISTANT_PAST,
        version = version
    )

    override fun taskInfosByTag(
        name: String
    ): Flow<List<Info>> = taskDao.taskInfoByTag(name).distinctUntilChanged().map { map ->
        map.map { (value, tags) ->
            value.toInfo(tags.map { it.name }.toSet())
        }
    }

    override fun taskInfos(
        query: TaskInfoQuery
    ): Flow<List<Info>> = taskDao.taskInfoByRawQuery(
        query = TaskQuery(
            ids = query.ids.toList(),
            tags = query.tags.toList(),
            states = query.states.map { it.toEntityState() },
            uniqueNames = query.uniqueNames.toList(),
        ).toRoomRawQuery()
    ).distinctUntilChanged().map { map ->
        map.map { (value, tags) ->
            value.toInfo(tags.map { it.name }.toSet())
        }
    }

    override fun taskInfoById(id: Uuid): Flow<Info?> = taskDao.taskInfoById(id).distinctUntilChanged().map { map ->
        map.map { (value, tags) ->
            value.toInfo(tags.map { it.name }.toSet())
        }.firstOrNull()
    }

    override fun taskInfoByIds(ids: Set<Uuid>) = taskDao.taskInfoByIds(ids).distinctUntilChanged().map { tasks ->
        tasks.map { it.toInfo(emptySet()) }
    }

    override suspend fun schedulableTasks(states: List<State>): List<SchedulableTask> = taskDao.schedulableTasks(
        states = states.map { it.toEntityState() }
    ).map {
        SchedulableTask(
            id = it.id,
            processTime = it.processTime,
            requiresDeviceIdle = it.requiresDeviceIdle,
            networkRequired = it.networkRequired
        )
    }

    override suspend fun childrenForTask(id: Uuid): List<Uuid> = taskParentTaskDao.childrenForTask(id)

    override suspend fun taskIdsByTagAndState(
        states: List<State>,
        tag: String
    ): List<Uuid> = taskDao.taskIdsByTagAndState(
        states = states.map { it.toEntityState() },
        tag = tag
    )

    override suspend fun resetState(
        from: State,
        to: State,
        excludedIds: Set<Uuid>
    ) {
        if (excludedIds.isEmpty()) taskDao.resetState(
            from = from.toEntityState(),
            to = to.toEntityState()
        ) else taskDao.resetStateWithExclusion(
            from = from.toEntityState(),
            to = to.toEntityState(),
            excludedIds = excludedIds
        )
    }

    override suspend fun updateProgressData(
        id: Uuid,
        progressData: ByteArray?
    ) = taskDao.updateProgressData(id, progressData)


    override suspend fun updateState(
        id: Uuid,
        state: State,
        allowedSourceStates: Set<State>,
        resetProcessTime: Boolean,
        runAttemptCount: Int?
    ) {
        taskDao.updateState(
            id = id,
            state = state.toEntityState(),
            allowedSourceStates = allowedSourceStates.map { it.toEntityState() },
            resetProcessTime = resetProcessTime,
            runAttemptCount = runAttemptCount
        )
    }

    override suspend fun updateTerminatingStateWithDescendants(
        id: Uuid,
        state: State,
        allowedSourceStates: Set<State>,
        finishedAt: Instant
    ) = taskDao.updateTerminatingStateWithAllDescendants(
        taskId = id,
        state = state.toEntityState(),
        allowedSourceStates = allowedSourceStates.map { it.toEntityState() },
        finishedAt = finishedAt
    )

    override suspend fun upgradeData(
        id: Uuid,
        input: ByteArray?,
        output: ByteArray?,
        progress: ByteArray?,
        version: Int
    ) = taskDao.upgradeData(
        taskId = id,
        input = input,
        output = output,
        progress = progress,
        version = version
    )

    override suspend fun <T> withTransaction(
        action: suspend Repository.() -> T
    ) = db.useWriterConnection {
        it.immediateTransaction {
            action()
        }
    }
}