package eu.tintera.background.tasks.core.data

import eu.tintera.background.tasks.State
import eu.tintera.background.tasks.TaskInfoQuery
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant
import kotlin.uuid.Uuid

interface Repository {

    suspend fun updateTerminatingState(
        id: Uuid,
        state: State,
        finishedAt: Instant,
        outputData: ByteArray?,
    )

    suspend fun task(id: Uuid): Task?

    suspend fun allByUniqueName(uniqueName: String, states: Set<State>): List<Uuid>
    suspend fun delete(id: Uuid)
    suspend fun insert(task: Task, tags: Set<String>, parentIds: Set<Uuid>)
    suspend fun cleanOld(terminalStates: Set<State>)

    fun taskInfosByTag(name: String): Flow<List<Info>>
    fun taskInfos(
        ids: Set<Uuid>,
        tags: Set<String>,
        states: Set<State>,
        uniqueNames: Set<String>
    ): Flow<List<Info>>
    fun taskInfoById(id: Uuid): Flow<Info?>

    fun taskInfoByIds(ids: Set<Uuid>): Flow<List<Info>>

    suspend fun childrenForTask(id: Uuid): List<Uuid>

    suspend fun taskIdsByTagAndState(states: List<State>, tag: String): List<Uuid>

    suspend fun updateState(
        id: Uuid,
        state: State,
        allowedSourceStates: Set<State>,
        resetProcessTime: Boolean,
        runAttemptCount: Int?
    )

    suspend fun finishTaskWithUnsuccess(id: Uuid, state: State, finishedAt: Instant)
}