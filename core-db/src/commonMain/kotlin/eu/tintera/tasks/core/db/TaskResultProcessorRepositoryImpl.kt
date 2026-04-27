package eu.tintera.tasks.core.db

import eu.tintera.tasks.State
import eu.tintera.tasks.core.TaskResultProcessorRepository
import eu.tintera.tasks.db.dao.TaskResultDao
import eu.tintera.tasks.db.toEntityState
import kotlin.time.Instant
import kotlin.uuid.Uuid

internal class TaskResultProcessorRepositoryImpl(
    private val dao: TaskResultDao
) : TaskResultProcessorRepository {

    override suspend fun scheduleNextFromBeginning(
        id: Uuid,
        state: State,
        allowedSourceStates: Set<State>,
        processTime: Instant
    ) {
        dao.scheduleNextFromBeginning(
            id = id,
            state = state.toEntityState(),
            processTime =  processTime,
            allowedSourceStates = allowedSourceStates.map { it.toEntityState() }
        )
    }

    override suspend fun scheduleNext(
        id: Uuid,
        state: State,
        allowedSourceStates: Set<State>,
        processTime: Instant
    ) {
        dao.scheduleNext(
            id = id,
            state = state.toEntityState(),
            processTime =  processTime,
            allowedSourceStates = allowedSourceStates.map { it.toEntityState() }
        )
    }

    override suspend fun failTask(
        id: Uuid,
        state: State,
        allowedSourceStates: Set<State>,
        finishedAt: Instant
    ) {
        dao.finishTaskWithUnsuccess(
            taskId = id,
            state = state.toEntityState(),
            finishedAt = finishedAt,
            allowedSourceStates = allowedSourceStates.map { it.toEntityState() }
        )
    }

    override suspend fun successTask(
        id: Uuid,
        state: State,
        allowedSourceStates: Set<State>,
        finishedAt: Instant,
        outputData: ByteArray
    ) {
        dao.finishTaskWithSuccess(
            id = id,
            state = state.toEntityState(),
            finishedAt = finishedAt,
            outputData = outputData,
            allowedSourceStates = allowedSourceStates.map { it.toEntityState() }
        )
    }
}