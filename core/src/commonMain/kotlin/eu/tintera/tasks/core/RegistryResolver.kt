package eu.tintera.tasks.core

import eu.tintera.tasks.Tag
import eu.tintera.tasks.TaskRegistration
import kotlin.reflect.KClass

interface RegistryResolver {
    suspend fun <I : Any, O : Any, P : Any> resolve(
        identifier: String
    ): TaskRegistration<I, O, P>?

    suspend fun <T: Tag> resolveTag(
        identifier: String
    ) : TagRegistration<T>?

    suspend fun <T : Tag> resolveTag(
        type: KClass<out T>
    ): TagRegistration<T>?
}