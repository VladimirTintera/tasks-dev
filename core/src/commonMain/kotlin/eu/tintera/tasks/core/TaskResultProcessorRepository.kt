package eu.tintera.tasks.core

import eu.tintera.tasks.State
import kotlin.time.Instant
import kotlin.uuid.Uuid

interface TaskResultProcessorRepository {
    suspend fun scheduleNextFromBeginning(id: Uuid, state: State, processTime: Instant)
    suspend fun scheduleNext(id: Uuid, state: State, processTime: Instant)

    suspend fun failTask(id: Uuid, state: State, finishedAt: Instant)
    suspend fun successTask(id: Uuid, state: State, finishedAt: Instant, outputData: ByteArray)
}