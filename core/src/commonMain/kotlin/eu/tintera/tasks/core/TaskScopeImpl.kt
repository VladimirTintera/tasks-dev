package eu.tintera.tasks.core

import eu.tintera.tasks.ForegroundInfo
import eu.tintera.tasks.ParentData
import eu.tintera.tasks.TaskScope
import eu.tintera.tasks.core.data.Repository
import eu.tintera.tasks.serialization.Serializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds
import kotlin.uuid.Uuid


class TaskScopeFactory(
    private val repository: Repository
) {
    fun <Input: Any, Progress: Any> createForTask(
        taskId: Uuid,
        data: Input,
        retryCount: Int,
        parentData: List<ParentData>,
        onForegroundInfoProvided: suspend (ForegroundInfo) -> Boolean,
        progressSerializer: Serializer<Progress>,
        scope: CoroutineScope
    ) = TaskScopeImpl(
        repository = repository,
        taskId = taskId,
        data = data,
        retryCount = retryCount,
        parents = parentData,
        onForegroundInfoProvided = onForegroundInfoProvided,
        progressSerializer = progressSerializer,
        scope = scope
    )
}

class TaskScopeImpl<Input: Any, Progress: Any>(
    override val taskId: Uuid,
    override val data: Input,
    override val retryCount: Int,
    override val parents: List<ParentData>,
    private val onForegroundInfoProvided: suspend (ForegroundInfo) -> Boolean,
    scope: CoroutineScope,
    private val repository: Repository,
    private val progressSerializer: Serializer<Progress>
) : TaskScope<Input, Progress> {

    private val progress = MutableStateFlow<Progress?>(null)
    @OptIn(FlowPreview::class)
    private val job = scope.launch {
        progress.filterNotNull().sample(300.milliseconds).collect {
            repository.updateProgressData(
                id = taskId,
                progressData = progressSerializer.encodeToBytes(it)
            )
        }
    }

    override suspend fun setForegroundInfo(
        foregroundInfo: ForegroundInfo
    ) : Boolean {
        return onForegroundInfoProvided(foregroundInfo)
    }

    override suspend fun setProgress(data: Progress) {
        progress.update { data }
    }

    suspend fun flushProgressAndClose() {

        job.cancel()

        withContext(NonCancellable) {
            progress.value?.also {
                repository.updateProgressData(taskId, progressSerializer.encodeToBytes(it))
            }
        }
    }
}