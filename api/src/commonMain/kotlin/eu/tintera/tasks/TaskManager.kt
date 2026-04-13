package eu.tintera.tasks

import kotlinx.coroutines.flow.Flow
import kotlin.reflect.KClass
import kotlin.time.Duration
import kotlin.uuid.Uuid

interface TaskManager {
    fun register(
        identifier: String,
        factory: () -> TaskHandler
    )

    suspend fun enqueueUniqueTask(
        task: TaskRequest,
        uniqueName: String = task.identifier,
        existingTaskPolicy: ExistingTaskPolicy = ExistingTaskPolicy.Keep
    ) : Uuid

    suspend fun enqueueTask(
        task: TaskRequest
    ) : Uuid

    suspend fun enqueueContinuation(
        continuation: TaskContinuation
    )

    suspend fun enqueueUniqueContinuation(
        continuation: TaskContinuation,
        uniqueName: String,
        existingTaskPolicy: ExistingTaskPolicy = ExistingTaskPolicy.Keep
    )

    suspend fun enqueuePeriodicUniqueTask(
        task: TaskRequest,
        repeatInterval: Duration,
        uniqueName: String = task.identifier,
        existingTaskPolicy: ExistingPeriodicTaskPolicy = ExistingPeriodicTaskPolicy.Keep
    ) : Uuid

    fun taskInfoById(id: Uuid): Flow<TaskInfo?>
    fun taskInfosByTag(tag: String): Flow<List<TaskInfo>>

    suspend fun cancelTaskById(id: Uuid)
    suspend fun cancelTasksByTag(tag: String)

    suspend fun cancelTask(type: KClass<out TaskHandler>) = cancelTasksByTag(type.fullName)

    companion object
}

fun TaskManager.register(
    type: KClass<out TaskHandler>,
    factory: () -> TaskHandler
) = register(type.fullName, factory)

inline fun <reified T : TaskHandler> TaskManager.register(
    noinline factory: () -> T
) = register(
    type = T::class,
    factory = factory
)

suspend inline fun <reified T : TaskHandler> TaskManager.cancelTask() = cancelTask(T::class)

