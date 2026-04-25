package eu.tintera.tasks.core.data

import eu.tintera.tasks.State
import eu.tintera.tasks.core.ProcessableTask
import kotlinx.coroutines.flow.Flow
import kotlin.uuid.Uuid

interface TaskProcessorRepository {
    fun processableTask(id: Uuid): Flow<ProcessableTask?>

    suspend fun executableTask(id: Uuid): ExecutableTask?

    suspend fun updateRunningState(id: Uuid, runAttemptCount: Int, allowedSourceStates: List<State>)
    suspend fun updateEnqueuedState(id: Uuid, allowedSourceStates: List<State>)
}