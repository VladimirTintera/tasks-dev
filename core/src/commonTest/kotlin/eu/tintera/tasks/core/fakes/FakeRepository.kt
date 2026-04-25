package eu.tintera.tasks.core.fakes

import eu.tintera.tasks.ParentData
import eu.tintera.tasks.State
import eu.tintera.tasks.TaskInfoQuery
import eu.tintera.tasks.core.data.*
import kotlinx.coroutines.flow.*
import kotlin.time.Instant
import kotlin.uuid.Uuid

class FakeRepository : Repository {

    private val tasks = MutableStateFlow<List<Task>>(emptyList())
    private val parentMap = mutableMapOf<Uuid, Set<Uuid>>() // childId -> parentIds
    override fun dispatchableTasks(states: List<State>): Flow<List<DispatchableTask>> {
        TODO("Not yet implemented")
    }

    override fun processableTask(id: Uuid): Flow<ProcessableTask?> {
        TODO("Not yet implemented")
    }

    override suspend fun executableTask(id: Uuid): ExecutableTask? {
        TODO("Not yet implemented")
    }

    override suspend fun parentsDataFor(id: Uuid): List<eu.tintera.tasks.core.data.ParentData> {
        val parentIds = parentMap[id] ?: emptySet()
        return tasks.map { allTasks ->
            allTasks.filter { it.id in parentIds }.map {
                ParentData(
                    id = it.id,
                    identifier = it.identifier,
                    outputData = it.outputData,
                    finishedAt = it.finishedAt ?: Instant.DISTANT_PAST,
                    version = it.version
                )
            }
        }.first()
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

    override suspend fun task(id: Uuid): Task? {
        return tasks.map { it.firstOrNull { it.id == id } }.firstOrNull()
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

    override fun taskInfosByTag(name: String): Flow<List<Info>> {
        TODO("Not yet implemented")
    }

    override fun taskInfos(query: TaskInfoQuery): Flow<List<Info>> {
        TODO("Not yet implemented")
    }

    override fun taskInfoById(id: Uuid): Flow<Info?> {
        TODO("Not yet implemented")
    }

    override fun taskInfoByIds(ids: Set<Uuid>): Flow<List<Info>> {
        TODO("Not yet implemented")
    }

    override suspend fun schedulableTasks(states: List<State>): List<SchedulableTask> {
        return tasks.map {
            it.filter { it.state in states }.map {
                SchedulableTask(
                    id = it.id,
                    processTime = it.processTime,
                    requiresDeviceIdle = it.requiresDeviceIdle,
                    networkRequired = it.networkRequired
                )
            }
        }.first()
    }

    override suspend fun childrenForTask(id: Uuid): List<Uuid> {
        TODO("Not yet implemented")
    }

    override suspend fun taskIdsByTagAndState(
        states: List<State>,
        tag: String
    ): List<Uuid> {
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
        resetProcessTime: Boolean,
        runAttemptCount: Int?
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

    override suspend fun updateTerminatingStateWithDescendants(
        id: Uuid,
        state: State,
        allowedSourceStates: Set<State>,
        finishedAt: Instant
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