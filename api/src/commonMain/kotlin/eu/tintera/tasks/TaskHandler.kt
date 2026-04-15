package eu.tintera.tasks

import kotlin.reflect.KClass

fun interface TaskHandler<Input, Output, Progress> {
    suspend fun TaskScope<Input, Progress>.run(): TaskResult<Output>
}

fun interface LegacyTaskHandler: TaskHandler<Data, Data, Data>

val KClass<out TaskHandler<*, *, *>>.fullName: String get() = qualifiedName ?: error("Anonymní třídy a lambdy nelze automaticky pojmenovat. Použij metodu register(identifier: String, ...) a zadej stabilní unikátní název.")
