package eu.tintera.tasks.core

import eu.tintera.tasks.ForegroundInfo
import eu.tintera.tasks.ParentData
import eu.tintera.tasks.Tag
import eu.tintera.tasks.TaskScope
import eu.tintera.tasks.core.data.TaskScopeRepository
import eu.tintera.tasks.serialization.Serializer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlin.time.Duration.Companion.milliseconds
import kotlin.uuid.Uuid


class TaskScopeFactory(
    private val repository: TaskScopeRepository
) {
    fun <Input : Any, Progress : Any> createForTask(
        taskId: Uuid,
        data: Input,
        retryCount: Int,
        parentData: List<ParentData>,
        onForegroundInfoProvided: suspend (ForegroundInfo) -> Boolean,
        progressSerializer: Serializer<Progress>,
        scope: CoroutineScope,
        tags: Set<String>,
        typedTags: Set<Tag>,
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
        typedTags = typedTags,
        saveDispatcher = saveDispatcher
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
    override val tags: Set<String>,
    override val typedTags: Set<Tag>,
    private val saveDispatcher: CoroutineDispatcher
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
            e.printStackTrace()
        }
    }

    override fun close() {
        job.cancel()
    }
}