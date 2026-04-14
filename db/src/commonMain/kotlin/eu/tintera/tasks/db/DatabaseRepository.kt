package eu.tintera.tasks.db

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import eu.tintera.tasks.Data
import eu.tintera.tasks.State
import eu.tintera.tasks.core.data.FullTask
import eu.tintera.tasks.core.data.Repository
import eu.tintera.tasks.core.data.Task
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

    override fun parentsFor(id: Uuid) = taskDao.parentsFor(id).map { list ->
        list.map { it.toTask() }
    }

    override suspend fun updateNextRun(
        id: Uuid,
        processTime: Instant,
        state: State,
        progressData: Data,
        runAttemptCount: Int?
    ) = taskDao.updateRetry(
        id = id,
        processTime = processTime,
        state = state.toEntityState(),
        progress = progressData.toSerializableTaskData(),
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
        outputData: Data
    ) = taskDao.updateTerminatingState(
        id = id,
        state = state.toEntityState(),
        finishedAt = finishedAt,
        outputData = outputData.toSerializableTaskData()
    )

    override suspend fun taskState(
        id: Uuid
    ): State? = taskDao.taskState(id)?.toTaskState()

    override fun task(
        id: Uuid
    ) = taskDao.task(id).map {
        it?.toTask()
    }

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
        states: Set<State>
    ) = taskDao.cleanOld(Clock.System.now().toEpochMilliseconds(), states.map { it.toEntityState() })

    override fun tasksByTag(
        name: String
    ): Flow<List<FullTask>> = taskDao.taskInfoByTag(name).distinctUntilChanged().map { map ->
        map.map { (value, tags) ->
            FullTask(
                task = value.toTask(),
                tags = tags.map { it.name }.toSet()
            )
        }
    }

    override fun taskById(id: Uuid): Flow<FullTask?> = taskDao.taskInfoById(id).distinctUntilChanged().map { map ->
        map.map { (value, tags) ->
            FullTask(
                task = value.toTask(),
                tags = tags.map { it.name }.toSet()
            )
        }.firstOrNull()
    }

    override fun tasksByIds(ids: Set<Uuid>)= taskDao.tasks(ids).distinctUntilChanged().map { tasks ->
        tasks.map { it.toTask() }
    }

    override fun tasksByState(states: List<State>): Flow<List<Task>> = taskDao.tasksByState(
        states = states.map { it.toEntityState() }
    ).map { tasks ->
        tasks.map { it.toTask() }
    }

    override suspend fun childrenForTask(id: Uuid): List<Uuid> = taskParentTaskDao.childrenForTask(id)
    override suspend fun tasksByTagAndState(
        states: List<State>,
        tag: String
    ): List<Task> = taskDao.tasksByTagAndState(
        states = states.map { it.toEntityState() },
        tag = tag
    ).map {
        it.toTask()
    }


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
        progressData: Data
    ) = taskDao.updateProgressData(id, progressData.toSerializableTaskData())

    override suspend fun updateState(
        id: Uuid,
        state: State,
        allowedSourceStates: Set<State>,
        resetProcessTime: Boolean
    ) {
        taskDao.updateState(
            id = id,
            state = state.toEntityState(),
            allowedSourceStates = allowedSourceStates.map { it.toEntityState() },
            resetProcessTime = resetProcessTime
        )
    }

    override suspend fun updateStateWithDescendants(
        id: Uuid,
        state: State,
        allowedSourceStates: Set<State>
    ) = taskDao.updateStateTaskAndAllDescendants(
        taskId = id,
        state = state.toEntityState(),
        allowedSourceStates = allowedSourceStates.map { it.toEntityState() }
    )

    override suspend fun <T> withTransaction(
        action: suspend Repository.() -> T
    ) = db.useWriterConnection {
        it.immediateTransaction {
            action()
        }
    }
}