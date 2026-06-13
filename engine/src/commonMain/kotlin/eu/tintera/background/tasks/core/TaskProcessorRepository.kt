package eu.tintera.background.tasks.core

import eu.tintera.background.tasks.State
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant
import kotlin.uuid.Uuid

interface TaskProcessorRepository {
    fun processableTask(id: Uuid): Flow<ProcessableTask?>

    suspend fun run(id: Uuid, allowedSourceStates: Set<State>)
    suspend fun updateEnqueuedState(id: Uuid, allowedSourceStates: Set<State>)

    suspend fun enqueue(id: Uuid, allowedSourceStates: Set<State>, processTime: Instant)
    suspend fun fail(id: Uuid)
}