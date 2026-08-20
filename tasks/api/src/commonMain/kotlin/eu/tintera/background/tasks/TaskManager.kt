package eu.tintera.background.tasks

import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration
import kotlin.uuid.Uuid

interface TaskManager {

    suspend fun <T : Any> enqueueUniqueTask(
        task: TaskRequest<T>,
        uniqueName: String = task.identifier,
        existingTaskPolicy: ExistingTaskPolicy = ExistingTaskPolicy.Keep
    ): Uuid

    suspend fun <T : Any> enqueueTask(
        task: TaskRequest<T>
    ): Uuid

    suspend fun enqueueContinuation(
        continuation: TaskContinuation
    )

    suspend fun enqueueUniqueContinuation(
        continuation: TaskContinuation,
        uniqueName: String,
        existingTaskPolicy: ExistingTaskPolicy = ExistingTaskPolicy.Keep
    )

    suspend fun <T : Any> enqueuePeriodicUniqueTask(
        task: TaskRequest<T>,
        repeatInterval: Duration,
        uniqueName: String = task.identifier,
        existingTaskPolicy: ExistingPeriodicTaskPolicy = ExistingPeriodicTaskPolicy.Keep
    ): Uuid

    fun taskInfoById(id: Uuid): Flow<TaskInfo?>
    fun taskInfosByTag(tag: String): Flow<List<TaskInfo>>

    suspend fun cancelTaskById(id: Uuid)
    suspend fun cancelTasksByTag(tag: String)

    /**
     * Cancels every unfinished task carrying [tag].
     *
     * The typed counterpart of the [String] overload, for tags declared through `taskTag(...)`.
     * Without it a caller holding a typed tag would have to reproduce the library's wire format
     * (`$tt:<identifier>:<payload>`) by hand, which defeats the point of typed tags: the moment the
     * format or the payload changes, the hand-built string silently stops matching anything.
     */
    suspend fun cancelTasksByTag(tag: Tag)
    fun taskInfos(query: TaskInfoQuery): Flow<List<TaskInfo>>

    companion object
}

fun TaskManager.taskInfos(block: TaskInfoQueryBuilder.() -> Unit) = taskInfos(
    TaskInfoQueryBuilder().apply(block).build()
)

