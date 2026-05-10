package eu.tintera.tasks.engine.db

import eu.tintera.tasks.State
import eu.tintera.tasks.core.ProcessableTask
import eu.tintera.tasks.core.TaskProcessorRepository
import eu.tintera.tasks.db.dao.TaskProcessorDao
import eu.tintera.tasks.db.toEntityState
import eu.tintera.tasks.db.toTaskBackoffCriteria
import eu.tintera.tasks.db.toTaskState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlin.time.Instant
import kotlin.uuid.Uuid

internal class TaskProcessorRepositoryImpl(
    private val dao: TaskProcessorDao,
) : TaskProcessorRepository {

    override fun processableTask(
        id: Uuid
    ) = dao.processableTask(id).distinctUntilChanged().map {
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

    override suspend fun run(
        id: Uuid,
        allowedSourceStates: Set<State>
    ) {
        dao.run(
            id = id,
            state = eu.tintera.tasks.db.State.Running,
            allowedSourceStates = allowedSourceStates.map { it.toEntityState() }
        )
    }

    override suspend fun updateEnqueuedState(id: Uuid, allowedSourceStates: Set<State>) {
        dao.updateEnqueuedState(
            id = id,
            allowedSourceStates = allowedSourceStates.map { it.toEntityState() },
            state = eu.tintera.tasks.db.State.Enqueued
        )
    }

    override suspend fun enqueue(
        id: Uuid,
        allowedSourceStates: Set<State>,
        processTime: Instant
    ) {
        dao.enqueue(
            id = id,
            state = eu.tintera.tasks.db.State.Enqueued,
            processTime =  processTime,
            allowedSourceStates = allowedSourceStates.map { it.toEntityState() }
        )
    }

    override suspend fun fail(id: Uuid) = dao.fail(
        id = id,
        state = eu.tintera.tasks.db.State.Failed
    )
}