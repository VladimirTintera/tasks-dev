package eu.tintera.tasks.core.data

import eu.tintera.tasks.State
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant
import kotlin.uuid.Uuid

interface Repository {

    fun dispatchableTasks(states: List<State>): Flow<List<DispatchableTask>>

    fun processableTask(id: Uuid): Flow<ProcessableTask?>

    suspend fun executableTask(id: Uuid) : ExecutableTask?

    suspend fun parentsDataFor(id: Uuid): List<ParentData>
    fun parentStatesForTask(id: Uuid): Flow<List<State>>
    suspend fun updateNextRun(
        id: Uuid,
        processTime: Instant,
        state: State,
        progressData: ByteArray?,
        runAttemptCount: Int?
    )

    suspend fun updateRunAttemptCount(id: Uuid, runAttemptsCount: Int)
    suspend fun updateTerminatingState(
        id: Uuid,
        state: State,
        finishedAt: Instant,
        outputData: ByteArray?,
    )

    suspend fun task(id: Uuid): Task?

    suspend fun allByUniqueName(uniqueName: String): List<Task>
    suspend fun delete(id: Uuid)
    suspend fun insert(task: Task, tags: Set<String>, parentIds: Set<Uuid>)
    suspend fun cleanOld(terminalStates: Set<State>)

    fun taskInfosByTag(name: String): Flow<List<Info>>
    fun taskInfoById(id: Uuid): Flow<Info?>

    fun taskInfoByIds(ids: Set<Uuid>): Flow<List<Info>>

    suspend fun schedulableTasks(states: List<State>): List<SchedulableTask>

    suspend fun childrenForTask(id: Uuid): List<Uuid>

    suspend fun taskIdsByTagAndState(states: List<State>, tag: String): List<Uuid>

    suspend fun resetState(from: State, to: State, excludedIds: Set<Uuid>)

    suspend fun updateProgressData(id: Uuid, progressData: ByteArray?)

    suspend fun updateState(
        id: Uuid,
        state: State,
        allowedSourceStates: Set<State>,
        resetProcessTime: Boolean,
        runAttemptCount: Int?
    )

    suspend fun updateTerminatingStateWithDescendants(id: Uuid, state: State, allowedSourceStates: Set<State>, finishedAt: Instant)

    suspend fun upgradeData(id: Uuid, input: ByteArray?, output: ByteArray?, progress: ByteArray?, version: Int)

    suspend fun <T> withTransaction(
        action: suspend Repository.() -> T
    ): T
}