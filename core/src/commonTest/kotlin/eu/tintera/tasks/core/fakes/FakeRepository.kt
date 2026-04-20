package eu.tintera.tasks.core.fakes

import eu.tintera.tasks.State
import eu.tintera.tasks.core.data.FullTask
import eu.tintera.tasks.core.data.Repository
import eu.tintera.tasks.core.data.Task
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlin.time.Instant
import kotlin.uuid.Uuid

class FakeRepository : Repository {

    private val tasks = MutableStateFlow<List<Task>>(emptyList())
    private val parentMap = mutableMapOf<Uuid, Set<Uuid>>() // childId -> parentIds

    override fun parentsFor(id: Uuid): Flow<List<Task>> {
        val parentIds = parentMap[id] ?: emptySet()
        return tasks.map { allTasks ->
            allTasks.filter { it.id in parentIds }
        }
    }

    override fun parentStatesForTask(id: Uuid): Flow<List<State>> {
        TODO("Not yet implemented")
    }

    override suspend fun updateRunAttemptCount(
        id: Uuid,
        runAttemptsCount: Int
    ) {
        tasks.update { currentTasks ->
            currentTasks.map {
                if (it.id == id) {
                    it.copy(
                        runAttemptCount = runAttemptsCount
                    )
                } else {
                    it
                }
            }
        }
    }

    override suspend fun updateNextRun(
        id: Uuid,
        processTime: Instant,
        state: State,
        progressData: ByteArray?,
        runAttemptCount: Int?
    ) {
        tasks.update { currentTasks ->
            currentTasks.map {
                if (it.id == id) {
                    it.copy(
                        state = state,
                        processTime = processTime,
                        progressData = progressData,
                        runAttemptCount = runAttemptCount ?: it.runAttemptCount
                    )
                } else {
                    it
                }
            }
        }
    }

    override suspend fun updateTerminatingState(
        id: Uuid,
        state: State,
        finishedAt: Instant,
        outputData: ByteArray?
    ) {
        tasks.update { currentTasks ->
            currentTasks.map {
                if (it.id == id) {
                    it.copy(
                        state = state,
                        finishedAt = finishedAt,
                        outputData = outputData
                    )
                } else {
                    it
                }
            }
        }
    }

    override suspend fun taskState(id: Uuid): State? {
        return tasks.value.firstOrNull {
            it.id == id
        }?.state
    }

    override fun task(id: Uuid): Flow<Task?> {
        return tasks.map { it.firstOrNull { it.id == id } }
    }

    override suspend fun allByUniqueName(uniqueName: String): List<Task> {
        TODO("Not yet implemented")
    }

    override suspend fun delete(id: Uuid) {
        TODO("Not yet implemented")
    }

    override suspend fun insert(
        task: Task,
        tags: Set<String>,
        parentIds: Set<Uuid>
    ) {
        tasks.update { currentTasks ->
            currentTasks + task
        }
        if (parentIds.isNotEmpty()) {
            parentMap[task.id] = parentIds
        }
    }

    override suspend fun cleanOld(
        terminalStates: Set<State>,
    ) {
        TODO("Not yet implemented")
    }

    override fun taskInfosByTag(name: String): Flow<List<FullTask>> {
        TODO("Not yet implemented")
    }

    override fun taskById(id: Uuid): Flow<FullTask?> {
        TODO("Not yet implemented")
    }

    override fun tasksByIds(ids: Set<Uuid>): Flow<List<Task>> {
        TODO("Not yet implemented")
    }

    override fun tasksByState(states: List<State>): Flow<List<Task>> {
        return tasks.map {
            it.filter { it.state in states }
        }
    }

    override suspend fun childrenForTask(id: Uuid): List<Uuid> {
        TODO("Not yet implemented")
    }

    override suspend fun tasksByTagAndState(
        states: List<State>,
        tag: String
    ): List<Task> {
        TODO("Not yet implemented")
    }

    override suspend fun resetState(
        from: State,
        to: State,
        excludedIds: Set<Uuid>
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun updateProgressData(
        id: Uuid,
        progressData: ByteArray?
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun updateState(
        id: Uuid,
        state: State,
        allowedSourceStates: Set<State>,
        resetProcessTime: Boolean
    ) {
        tasks.update {
            it.map {
                if (it.id == id && it.state in allowedSourceStates) {
                    it.copy(state = state)
                } else {
                    it
                }
            }
        }
    }

    override suspend fun updateStateWithDescendants(
        id: Uuid,
        state: State,
        allowedSourceStates: Set<State>
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun upgradeData(
        id: Uuid,
        input: ByteArray?,
        output: ByteArray?,
        progress: ByteArray?,
        version: Int
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun <T> withTransaction(action: suspend Repository.() -> T): T {
        TODO("Not yet implemented")
    }
}