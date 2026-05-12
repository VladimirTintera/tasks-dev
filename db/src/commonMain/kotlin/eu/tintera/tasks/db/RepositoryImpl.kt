package eu.tintera.tasks.db

import eu.tintera.tasks.State
import eu.tintera.tasks.core.data.*
import eu.tintera.tasks.core.runningStates
import eu.tintera.tasks.db.dao.TaskDao
import eu.tintera.tasks.db.dao.TaskResultDao
import eu.tintera.tasks.db.entities.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid

internal class RepositoryImpl(
    private val taskDao: TaskDao,
    private val taskResultDao: TaskResultDao,
    private val taskTagDao: TaskTagDao,
    private val taskParentTaskDao: TaskParentTaskDao,
    private val transactionRunner: TransactionRunner
) : Repository {

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
    ) = transactionRunner {
        taskDao.insert(
            task.toTaskEntity()
        )

        if (tags.isNotEmpty()) taskTagDao.insert(
            tags.map {
                TaskTagEntity(
                    taskId = task.id,
                    name = it
                )
            }
        )

        parentIds.forEach {
            taskParentTaskDao.insert(
                TaskParentTaskEntity(
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
    ): Flow<List<Info>> = taskDao.taskInfoByTag(name).onEach {
        println("Current tasks: $it")
    }.distinctUntilChanged().map { map ->
        map.map { (value, tags) ->
            value.toInfo(tags.map { it.name }.toSet())
        }
    }

    override fun taskInfos(
        ids: Set<Uuid>,
        tags: Set<String>,
        states: Set<State>,
        uniqueNames: Set<String>
    ): Flow<List<Info>> = taskDao.taskInfoByRawQuery(
        query = TaskQueryEntity(
            ids = ids.toList(),
            tags = tags.toList(),
            states = states.map { it.toEntityState() },
            uniqueNames = uniqueNames.toList(),
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

    override suspend fun childrenForTask(id: Uuid): List<Uuid> = taskParentTaskDao.childrenForTask(id)

    override suspend fun taskIdsByTagAndState(
        states: List<State>,
        tag: String
    ): List<Uuid> = taskDao.taskIdsByTagAndState(
        states = states.map { it.toEntityState() },
        tag = tag
    )

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

    override suspend fun finishTaskWithUnsuccess(
        id: Uuid,
        state: State,
        finishedAt: Instant
    ) = taskResultDao.finishTaskWithUnsuccess(
        taskId = id,
        state = state.toEntityState(),
        finishedAt = finishedAt,
        allowedSourceStates = runningStates.map { it.toEntityState() }
    )
}