package eu.tintera.background.tasks.core

import eu.tintera.background.tasks.State
import kotlin.time.Instant
import kotlin.uuid.Uuid

interface TaskResultProcessorRepository {
    suspend fun scheduleNextFromBeginning(id: Uuid, state: State, allowedSourceStates: Set<State>, processTime: Instant)
    suspend fun scheduleNext(id: Uuid, state: State, allowedSourceStates: Set<State>, processTime: Instant)

    suspend fun failTask(id: Uuid, state: State, allowedSourceStates: Set<State>, finishedAt: Instant)
    suspend fun successTask(id: Uuid, state: State, allowedSourceStates: Set<State>, finishedAt: Instant, outputData: ByteArray)
}