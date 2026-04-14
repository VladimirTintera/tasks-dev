package eu.tintera.tasks.core.data

import eu.tintera.tasks.Data
import eu.tintera.tasks.State
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant
import kotlin.uuid.Uuid

interface Repository {
    fun parentsFor(id: Uuid): Flow<List<Task>>
    suspend fun updateNextRun(id: Uuid, processTime: Instant, state: State, progressData: Data, runAttemptCount: Int?)

    suspend fun updateRunAttemptCount(id: Uuid, runAttemptsCount: Int)
    suspend fun updateTerminatingState(
        id: Uuid,
        state: State,
        finishedAt: Instant,
        outputData: Data,
    )

    suspend fun taskState(id: Uuid): State?

    fun task(id: Uuid): Flow<Task?>

    suspend fun allByUniqueName(uniqueName: String): List<Task>
    suspend fun delete(id: Uuid)
    suspend fun insert(task: Task, tags: Set<String>, parentIds: Set<Uuid>)
    suspend fun cleanOld(states: Set<State>)

    fun tasksByTag(name: String): Flow<List<FullTask>>

    fun taskById(id: Uuid): Flow<FullTask?>

    fun tasksByState(states: List<State>): Flow<List<Task>>

    suspend fun childrenForTask(id: Uuid): List<Uuid>

    suspend fun tasksByTagAndState(states: List<State>, tag: String): List<Task>

    suspend fun resetState(from: State, to: State, excludedIds: Set<Uuid>)

    suspend fun updateProgressData(id: Uuid, progressData: Data)

    suspend fun updateState(id: Uuid, state: State, allowedSourceStates: Set<State>)

    suspend fun updateStateWithDescendants(id: Uuid, state: State, allowedSourceStates: Set<State>)

    suspend fun <T> withTransaction(
        action: suspend Repository.() -> T
    ): T
}