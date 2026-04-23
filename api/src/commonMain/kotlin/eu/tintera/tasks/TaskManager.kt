package eu.tintera.tasks

import kotlinx.coroutines.flow.Flow
import kotlin.reflect.KClass
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

    suspend fun cancelTask(type: KClass<out TaskHandler<Any, Any, Any>>) = cancelTasksByTag(type.fullName)

    companion object
}


suspend inline fun <reified T : TaskHandler<Any, Any, Any>> TaskManager.cancelTask() = cancelTask(T::class)

