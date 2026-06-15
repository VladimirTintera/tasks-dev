package eu.tintera.background.tasks.core.fakes

import eu.tintera.background.tasks.State
import eu.tintera.background.tasks.core.OrphanTaskSweeperRepository
import eu.tintera.background.tasks.core.ProcessableTask
import eu.tintera.background.tasks.core.TaskDispatcherRepository
import eu.tintera.background.tasks.core.TaskProcessorRepository
import eu.tintera.background.tasks.core.TaskResultProcessorRepository
import eu.tintera.background.tasks.core.data.*
import kotlinx.coroutines.flow.*
import kotlin.time.Instant
import kotlin.uuid.Uuid

class FakeRepository : Repository, TaskDispatcherRepository, TaskProcessorRepository, TaskEvaluatorRepository, OrphanTaskSweeperRepository, TaskScopeRepository, TaskResultProcessorRepository {

    private val tasks = MutableStateFlow<List<Task>>(emptyList())

    fun taskState(id: Uuid): State? = tasks.value.firstOrNull { it.id == id }?.state
    private val parentMap = mutableMapOf<Uuid, Set<Uuid>>() // childId -> parentIds
    private val tagMap = mutableMapOf<Uuid, Set<String>>() // taskId -> tags

    override fun dispatchableTasks(states: Set<State>): Flow<List<DispatchableTask>> {
        return tasks.map { list ->
            list.filter { it.state in states }.map { DispatchableTask(it.id, it.state) }
        }
    }

    override fun processableTask(id: Uuid): Flow<ProcessableTask?> {
        return tasks.map { list ->
            list.firstOrNull { it.id == id }?.let {
                ProcessableTask(
                    id = it.id,
                    state = it.state,
                    initialDelay = it.initialDelay,
                    runAttemptCount = it.runAttemptCount,
                    networkRequired = it.networkRequired,
                    requiresDeviceIdle = it.requiresDeviceIdle,
                    repeatInterval = it.repeatInterval,
                    backoffCriteria = it.backoffCriteria,
                    processTime = it.processTime
                )
            }
        }
    }

    override suspend fun executableTask(id: Uuid): ExecutableTask? {
        return tasks.value.firstOrNull { it.id == id }?.let {
            ExecutableTask(
                identifier = it.identifier,
                runAttemptCount = it.runAttemptCount,
                backoffCriteria = it.backoffCriteria,
                repeatInterval = it.repeatInterval,
                version = it.version,
                inputData = it.inputData,
                outputData = it.outputData,
                progressData = it.progressData,
                tags = getTags(it.id)
            )
        }
    }

    override suspend fun parentsDataFor(id: Uuid): List<eu.tintera.background.tasks.core.data.ParentData> {
        val parentIds = parentMap[id] ?: emptySet()
        return tasks.value.filter { it.id in parentIds }.map {
            eu.tintera.background.tasks.core.data.ParentData(
                id = it.id,
                identifier = it.identifier,
                outputData = it.outputData,
                finishedAt = it.finishedAt ?: Instant.DISTANT_PAST,
                version = it.version
            )
        }
    }


    override suspend fun run(id: Uuid, allowedSourceStates: Set<State>) {
        val currentTask = tasks.value.firstOrNull { it.id == id }
        val nextRunAttemptCount = currentTask?.let { it.runAttemptCount + 1 }
        updateState(id, State.Running, allowedSourceStates, resetProcessTime = true, runAttemptCount = nextRunAttemptCount)
    }

    override suspend fun updateEnqueuedState(id: Uuid, allowedSourceStates: Set<State>) {
        updateState(id, State.Enqueued, allowedSourceStates, resetProcessTime = false, runAttemptCount = null)
    }

    override suspend fun enqueue(id: Uuid, allowedSourceStates: Set<State>, processTime: Instant) {
        updateState(id, State.Enqueued, allowedSourceStates, resetProcessTime = false, runAttemptCount = null)
        tasks.update { list ->
            list.map {
                if (it.id == id) it.copy(processTime = processTime) else it
            }
        }
    }

    override suspend fun fail(id: Uuid) {
        updateState(id, State.Failed, emptySet(), resetProcessTime = false, runAttemptCount = null)
    }

    override suspend fun upgradeData(
        id: Uuid,
        input: ByteArray?,
        output: ByteArray?,
        progress: ByteArray?,
        version: Int
    ) {
        tasks.update { list ->
            list.map {
                if (it.id == id) {
                    it.copy(
                        inputData = input,
                        outputData = output,
                        progressData = progress,
                        version = version
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

    override suspend fun cleanOld(terminalStates: Set<State>) {
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
                if (it.id == id && (allowedSourceStates.isEmpty() || it.state in allowedSourceStates)) {
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

    override suspend fun resetState(from: State, to: State, excludedIds: Set<Uuid>) {
        tasks.update { list ->
            list.map {
                if (it.state == from && it.id !in excludedIds) {
                    it.copy(state = to)
                } else {
                    it
                }
            }
        }
    }

    override suspend fun updateProgressData(id: Uuid, progressData: ByteArray?) {
        tasks.update { list ->
            list.map {
                if (it.id == id) it.copy(progressData = progressData) else it
            }
        }
    }

    override suspend fun scheduleNextFromBeginning(id: Uuid, state: State, allowedSourceStates: Set<State>, processTime: Instant) {
        updateState(id, state, allowedSourceStates, resetProcessTime = false, runAttemptCount = 0)
        tasks.update { list ->
            list.map {
                if (it.id == id) it.copy(processTime = processTime) else it
            }
        }
    }

    override suspend fun scheduleNext(id: Uuid, state: State, allowedSourceStates: Set<State>, processTime: Instant) {
        updateState(id, state, allowedSourceStates, resetProcessTime = false, runAttemptCount = null)
        tasks.update { list ->
            list.map {
                if (it.id == id) it.copy(processTime = processTime) else it
            }
        }
    }

    override suspend fun failTask(id: Uuid, state: State, allowedSourceStates: Set<State>, finishedAt: Instant) {
        updateState(id, state, allowedSourceStates, resetProcessTime = false, runAttemptCount = 0)
        tasks.update { list ->
            list.map {
                if (it.id == id) it.copy(finishedAt = finishedAt) else it
            }
        }
    }

    override suspend fun successTask(id: Uuid, state: State, allowedSourceStates: Set<State>, finishedAt: Instant, outputData: ByteArray) {
        updateState(id, state, allowedSourceStates, resetProcessTime = false, runAttemptCount = 0)
        tasks.update { list ->
            list.map {
                if (it.id == id) it.copy(finishedAt = finishedAt, outputData = outputData) else it
            }
        }
    }
}