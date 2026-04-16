package eu.tintera.tasks

import kotlin.reflect.KClass

fun interface TaskHandler<Input, Output, Progress> {
    suspend fun TaskScope<Input, Progress>.run(): TaskResult<Output>
}


@Deprecated("Use typed RegistrationHandler instead")
typealias LegacyTaskHandler = TaskHandler<Data, Data, Data>

val KClass<out TaskHandler<*, *, *>>.fullName: String get() = qualifiedName ?: error("Anonymous class and lambda could not be registered by type. Use registration with identifier instead.")
