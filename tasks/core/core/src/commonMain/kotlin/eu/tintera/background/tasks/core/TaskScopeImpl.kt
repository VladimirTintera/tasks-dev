package eu.tintera.background.tasks.core

import eu.tintera.background.tasks.ForegroundInfo
import eu.tintera.background.tasks.ParentData
import eu.tintera.background.tasks.Tag
import eu.tintera.background.tasks.TaskScope
import eu.tintera.background.tasks.core.data.TaskScopeRepository
import eu.tintera.background.tasks.serialization.Serializer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlin.time.Duration.Companion.milliseconds
import kotlin.uuid.Uuid


class TaskScopeFactory(
    private val repository: TaskScopeRepository,
    private val log: CompositeTasksLogger
) {
    fun <Input : Any, Progress : Any> createForTask(
        taskId: Uuid,
        data: Input,
        retryCount: Int,
        parentData: List<ParentData>,
        onForegroundInfoProvided: suspend (ForegroundInfo) -> Boolean,
        progressSerializer: Serializer<Progress>,
        scope: CoroutineScope,
        tags: Set<Tag>,
        saveDispatcher: CoroutineDispatcher
    ) = TaskScopeImpl(
        repository = repository,
        taskId = taskId,
        data = data,
        retryCount = retryCount,
        parents = parentData,
        onForegroundInfoProvided = onForegroundInfoProvided,
        progressSerializer = progressSerializer,
        scope = scope,
        tags = tags,
        saveDispatcher = saveDispatcher,
        log = log
    )
}

class TaskScopeImpl<Input : Any, Progress : Any>(
    override val taskId: Uuid,
    override val data: Input,
    override val retryCount: Int,
    override val parents: List<ParentData>,
    private val onForegroundInfoProvided: suspend (ForegroundInfo) -> Boolean,
    scope: CoroutineScope,
    private val repository: TaskScopeRepository,
    private val progressSerializer: Serializer<Progress>,
    override val tags: Set<Tag>,
    private val saveDispatcher: CoroutineDispatcher,
    private val log: CompositeTasksLogger
) : TaskScope<Input, Progress>, AutoCloseable {

    private val progress = MutableStateFlow<Progress?>(null)

    @OptIn(FlowPreview::class)
    private val job = scope.launch {
        progress.filterNotNull().collect {
            trySave(it)
            delay(300.milliseconds)
        }
    }

    override suspend fun setForegroundInfo(
        foregroundInfo: ForegroundInfo
    ): Boolean {
        return onForegroundInfoProvided(foregroundInfo)
    }

    override suspend fun setProgress(data: Progress) {
        progress.update { data }
    }

    suspend fun flushProgress() {
        withContext(NonCancellable) {
            progress.value?.also {
                trySave(it)
            }
        }
    }

    private suspend fun trySave(progress: Progress) {
        try {
            withContext(saveDispatcher) {
                repository.updateProgressData(taskId, progressSerializer.encodeToBytes(progress))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Progress is informational only — failing to store it must not bring down a running task.
            log.warning(TAG, e) { "Failed to persist progress for task $taskId" }
        }
    }

    override fun close() {
        job.cancel()
    }

    private companion object {
        private const val TAG = "TaskScope"
    }
}