package eu.tintera.tasks.db

import eu.tintera.tasks.State
import eu.tintera.tasks.core.data.ExecutableTask
import eu.tintera.tasks.core.data.ProcessableTask
import eu.tintera.tasks.core.data.TaskProcessorRepository
import eu.tintera.tasks.db.dao.TaskProcessorDao
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlin.uuid.Uuid

internal class TaskProcessorRepository(
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

    override suspend fun executableTask(id: Uuid): ExecutableTask? =
        dao.getExecutableTasksById(id)?.map { (task, tags) ->
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

    override suspend fun updateRunningState(
        id: Uuid,
        runAttemptCount: Int,
        allowedSourceStates: List<State>
    ) {
        dao.updateRunningState(
            id = id,
            state = eu.tintera.tasks.db.State.Running,
            allowedSourceStates = allowedSourceStates.map { it.toEntityState() },
            runAttemptCount = runAttemptCount
        )
    }

    override suspend fun updateEnqueuedState(id: Uuid, allowedSourceStates: List<State>) {
        dao.updateEnqueuedState(
            id = id,
            allowedSourceStates = allowedSourceStates.map { it.toEntityState() },
            state = eu.tintera.tasks.db.State.Enqueued
        )
    }
}