package eu.tintera.tasks

import kotlin.reflect.KClass

fun interface TaskHandler<Input: Any, Output: Any, Progress: Any> {
    suspend fun TaskScope<Input, Progress>.run(): TaskResult<Output>
}

val KClass<out TaskHandler<*, *, *>>.fullName: String get() = qualifiedName ?: error("Anonymous class and lambda could not be registered by type. Use registration with identifier instead.")
