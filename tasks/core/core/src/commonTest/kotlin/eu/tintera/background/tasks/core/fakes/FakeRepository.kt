package eu.tintera.background.tasks.core.fakes

import eu.tintera.background.tasks.State
import eu.tintera.background.tasks.core.data.*
import kotlinx.coroutines.flow.*
import kotlin.time.Instant
import kotlin.uuid.Uuid

class FakeRepository : Repository, TaskScopeRepository {

    private val tasks = MutableStateFlow<List<Task>>(emptyList())

    fun taskState(id: Uuid): State? = tasks.value.firstOrNull { it.id == id }?.state
    private val parentMap = mutableMapOf<Uuid, Set<Uuid>>() // childId -> parentIds
    private val tagMap = mutableMapOf<Uuid, Set<String>>() // taskId -> tags

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
        return tasks.value.firstOrNull { it.id == id }
    }

    override suspend fun allByUniqueName(uniqueName: String, states: Set<State>): List<Uuid> {
        return tasks.value.filter { it.uniqueName == uniqueName && it.state in states }.map { it.id }
    }

    override suspend fun delete(id: Uuid) {
        tasks.update { it.filterNot { task -> task.id == id } }
        parentMap.remove(id)
        tagMap.remove(id)
    }

    override suspend fun insert(
        task: Task,
        tags: Set<String>,
        parentIds: Set<Uuid>
    ) {
        tasks.update { it + task }
        if (parentIds.isNotEmpty()) {
            parentMap[task.id] = parentIds
        }
        if (tags.isNotEmpty()) {
            tagMap[task.id] = tags
        }
    }

    override suspend fun cleanOld(
        terminalStates: Set<State>
    ) {
        tasks.update { it.filterNot { task -> task.state in terminalStates } }
    }

    private fun getTags(id: Uuid): Set<String> = tagMap[id] ?: emptySet()

    private fun toInfo(task: Task) = Info(
        id = task.id,
        identifier = task.identifier,
        runAttemptCount = task.runAttemptCount,
        state = task.state,
        tags = getTags(task.id),
        outputData = task.outputData,
        processTime = task.processTime,
        progressData = task.progressData,
        finishedAt = task.finishedAt,
        createdAt = task.createdAt,
        version = task.version
    )

    override fun taskInfosByTag(name: String): Flow<List<Info>> {
        return tasks.map { list ->
            list.filter { name in getTags(it.id) }.map { toInfo(it) }
        }
    }

    override fun taskInfos(
        ids: Set<Uuid>,
        tags: Set<String>,
        states: Set<State>,
        uniqueNames: Set<String>
    ): Flow<List<Info>> {
        return tasks.map { list ->
            list.filter { task ->
                (ids.isEmpty() || task.id in ids) &&
                (tags.isEmpty() || getTags(task.id).any { it in tags }) &&
                (states.isEmpty() || task.state in states) &&
                (uniqueNames.isEmpty() || task.uniqueName in uniqueNames)
            }.map { toInfo(it) }
        }
    }

    override fun taskInfoById(id: Uuid): Flow<Info?> {
        return tasks.map { list ->
            list.firstOrNull { it.id == id }?.let { toInfo(it) }
        }
    }

    override fun taskInfoByIds(ids: Set<Uuid>): Flow<List<Info>> {
        return tasks.map { list ->
            list.filter { it.id in ids }.map { toInfo(it) }
        }
    }

    override suspend fun childrenForTask(id: Uuid): List<Uuid> {
        return parentMap.filterValues { id in it }.keys.toList()
    }

    override suspend fun taskIdsByTagAndState(
        states: List<State>,
        tag: String
    ): List<Uuid> {
        return tasks.value.filter { it.state in states && tag in getTags(it.id) }.map { it.id }
    }

    override suspend fun updateState(
        id: Uuid,
        state: State,
        allowedSourceStates: Set<State>,
        resetProcessTime: Boolean,
        runAttemptCount: Int?
    ) {
        tasks.update { list ->
            list.map {
                if (it.id == id && it.state in allowedSourceStates) {
                    it.copy(
                        state = state,
                        processTime = if (resetProcessTime) null else it.processTime,
                        runAttemptCount = runAttemptCount ?: it.runAttemptCount
                    )
                } else {
                    it
                }
            }
        }
    }

    override suspend fun finishTaskWithUnsuccess(
        id: Uuid,
        state: State,
        finishedAt: Instant
    ) {
        tasks.update { list ->
            list.map {
                if (it.id == id) {
                    it.copy(state = state, finishedAt = finishedAt)
                } else {
                    it
                }
            }
        }
    }

    override suspend fun updateProgressData(id: Uuid, progressData: ByteArray?) {
        tasks.update { list ->
            list.map {
                if (it.id == id) {
                    it.copy(progressData = progressData)
                } else {
                    it
                }
            }
        }
    }
}