package eu.tintera.tasks

import kotlin.uuid.Uuid

open class TaskGraphException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class ParentTaskNotFoundException(message: String) : TaskGraphException(message)
class TaskTypeMismatchException(message: String) : TaskGraphException(message)

interface TaskScope<Input: Any, Progress: Any> : InputTaskScope<Input> {
    suspend fun setProgress(data: Progress)
}

interface SimpleTaskScope: TaskScope<Unit, Unit>
interface InputTaskScope<T: Any> {
    val taskId: Uuid
    val data: T
    val retryCount: Int

    val parents: List<ParentData>

    val tags: Set<String>
    val typedTags: Set<Tag>

    suspend fun setForegroundInfo(foregroundInfo: ForegroundInfo): Boolean
}

inline fun <reified T> TaskScope<*, *>.parentOutputs(
    identifier: String
): List<T> = parents
    .filter { it.identifier == identifier }
    .map {
        it.data as? T ?: throw TaskTypeMismatchException(
            "Type mismatch in parent task '$identifier'! " +
                    "Expected type '${T::class.simpleName}', but received '${it.data?.let { d -> d::class.simpleName } ?: "null"}'."
        )
    }

inline fun <reified T> TaskScope<*, *>.parentOutput(
    identifier: String
): T = parentOutputs<T>(identifier).firstOrNull()
    ?: throw ParentTaskNotFoundException("Parent task '$identifier' not found. Make sure it is correctly connected in the graph!")
inline fun <reified T> TaskScope<*, *>.parentOutputOrNull(
    identifier: String
): T? = parentOutputs<T>(identifier).firstOrNull()


inline fun <reified T, reified R : TaskHandler<out Any, out Any, out Any>> TaskScope<*, *>.parentOutputs(): List<T> =
    parentOutputs(R::class.fullName)

inline fun <reified T, reified R : TaskHandler<out Any, out Any, out Any>> TaskScope<*, *>.parentOutput(): T =
    parentOutput(R::class.fullName)

inline fun <reified T, reified R : TaskHandler<out Any, out Any, out Any>> TaskScope<*, *>.parentOutputOrNull(): T? =
    parentOutputOrNull(R::class.fullName)

inline fun <reified T : Any> TaskScope<*, *>.parentOutputsOfType(): List<T> =
    parents.mapNotNull { it.data as? T }

inline fun <reified T : Any> TaskScope<*, *>.parentOutputOfType(): T =
    parentOutputsOfType<T>().firstOrNull()
        ?: throw ParentTaskNotFoundException("No parent task returning type '${T::class.simpleName}' was found in the graph!")

inline fun <reified T : Any> TaskScope<*, *>.parentOutputOfTypeOrNull(): T? =
    parentOutputsOfType<T>().firstOrNull()


inline fun <reified T : Any> TaskScope<*, *>.latestParentOutputOfType(): T =
    latestParentOutputOfTypeOrNull<T>()
        ?: throw ParentTaskNotFoundException("No parent task returning type '${T::class.simpleName}' was found in the graph!")

inline fun <reified T : Any> TaskScope<*, *>.latestParentOutputOfTypeOrNull(): T? =
    parents
        .filter { it.data is T }
        .maxByOrNull { it.finishedAt }
        ?.let { it.data as T }
