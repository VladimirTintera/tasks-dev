package eu.tintera.tasks.core

import eu.tintera.tasks.Tag
import eu.tintera.tasks.TaskHandler
import eu.tintera.tasks.TaskRegistration
import kotlin.reflect.KClass

interface RegistryResolver {
    suspend fun <I : Any, O : Any, P : Any> resolve(
        identifier: String
    ): TaskRegistration<I, O, P>?

    suspend fun <T : TaskHandler<I, O, P>, I : Any, O : Any, P : Any> resolve(
        type: KClass<out T>
    ): List<TaskRegistration<I, O, P>>?

    suspend fun <T : Tag> resolveTag(
        identifier: String
    ): TagRegistration<T>?

    suspend fun <T : Tag> resolveTag(
        type: KClass<out T>
    ): TagRegistration<T>?
}