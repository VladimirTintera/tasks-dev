package eu.tintera.tasks

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import kotlin.reflect.KClass
import kotlin.time.Duration
import kotlin.uuid.Uuid

interface TaskManager {
    fun <Input, Output, Progress> register(
        identifier: String,
        currentVersion: Int,
        factory: () -> TaskHandler<Input, Output, Progress>,
        inputSerializer: KSerializer<Input>,
        outputSerializer: KSerializer<Output>,
        progressSerializer: KSerializer<Progress>
    )

    suspend fun enqueueUniqueTask(
        task: TaskRequest<*>,
        uniqueName: String = task.identifier,
        existingTaskPolicy: ExistingTaskPolicy = ExistingTaskPolicy.Keep
    ): Uuid

    suspend fun enqueueTask(
        task: TaskRequest<*>
    ): Uuid

    suspend fun enqueueContinuation(
        continuation: TaskContinuation
    )

    suspend fun enqueueUniqueContinuation(
        continuation: TaskContinuation,
        uniqueName: String,
        existingTaskPolicy: ExistingTaskPolicy = ExistingTaskPolicy.Keep
    )

    suspend fun enqueuePeriodicUniqueTask(
        task: TaskRequest<*>,
        repeatInterval: Duration,
        uniqueName: String = task.identifier,
        existingTaskPolicy: ExistingPeriodicTaskPolicy = ExistingPeriodicTaskPolicy.Keep
    ): Uuid

    fun taskInfoById(id: Uuid): Flow<TaskInfo?>
    fun taskInfosByTag(tag: String): Flow<List<TaskInfo>>

    suspend fun cancelTaskById(id: Uuid)
    suspend fun cancelTasksByTag(tag: String)

    suspend fun cancelTask(type: KClass<out TaskHandler<*, *, *>>) = cancelTasksByTag(type.fullName)

    companion object
}

inline fun <reified I, reified O, reified P> TaskManager.register(
    identifier: String,
    currentVersion: Int = 1,
    noinline factory: () -> TaskHandler<I, O, P>
) {
    register(
        identifier = identifier,
        currentVersion = currentVersion,
        factory = factory,
        inputSerializer = serializer<I>(),
        outputSerializer = serializer<O>(),
        progressSerializer = serializer<P>()
    )
}

inline fun <reified T : TaskHandler<I, O, P>, reified I, reified O, reified P> TaskManager.register(
    currentVersion: Int = 1,
    noinline factory: () -> T
) = register(
    identifier = T::class.fullName,
    currentVersion = currentVersion,
    factory = factory
)




suspend inline fun <reified T : TaskHandler<*, *, *>> TaskManager.cancelTask() = cancelTask(T::class)

