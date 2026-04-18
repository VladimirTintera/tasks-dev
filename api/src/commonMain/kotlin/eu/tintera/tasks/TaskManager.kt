package eu.tintera.tasks

import eu.tintera.tasks.migrations.Migration
import eu.tintera.tasks.serialization.TaskDataSerializer
import kotlinx.coroutines.flow.Flow
import kotlin.reflect.KClass
import kotlin.time.Duration
import kotlin.uuid.Uuid

interface TaskManager {
    fun <Input: Any, Output: Any, Progress: Any> register(
        identifier: String,
        currentVersion: Int = 1,
        factory: () -> TaskHandler<Input, Output, Progress>,
        inputSerializer: TaskDataSerializer<Input>,
        outputSerializer: TaskDataSerializer<Output>,
        progressSerializer: TaskDataSerializer<Progress>,
        migrations: List<Migration> = emptyList()
    )

    suspend fun <T: Any> enqueueUniqueTask(
        task: TaskRequest<T>,
        uniqueName: String = task.identifier,
        existingTaskPolicy: ExistingTaskPolicy = ExistingTaskPolicy.Keep
    ): Uuid

    suspend fun <T: Any> enqueueTask(
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

    suspend fun <T: Any> enqueuePeriodicUniqueTask(
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

inline fun <reified T : TaskHandler<I, O, P>, reified I: Any, reified O: Any, reified P: Any> TaskManager.register(
    currentVersion: Int = 1,
    inputSerializer: TaskDataSerializer<I>,
    outputSerializer: TaskDataSerializer<O>,
    progressSerializer: TaskDataSerializer<P>,
    migrations: List<Migration> = emptyList(),
    noinline factory: () -> T
) = register(
    identifier = T::class.fullName,
    currentVersion = currentVersion,
    factory = factory,
    inputSerializer = inputSerializer,
    outputSerializer = outputSerializer,
    progressSerializer = progressSerializer,
    migrations = migrations,
)


suspend inline fun <reified T : TaskHandler<Any, Any, Any>> TaskManager.cancelTask() = cancelTask(T::class)

