package eu.tintera.tasks

import kotlin.reflect.KClass

fun interface TaskHandler {
    suspend fun TaskScope.run(): TaskResult
}

val KClass<out TaskHandler>.fullName: String get() = qualifiedName ?: "KClass@${hashCode()}"