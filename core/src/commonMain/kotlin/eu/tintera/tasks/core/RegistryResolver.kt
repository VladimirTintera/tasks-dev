package eu.tintera.tasks.core

import eu.tintera.tasks.TaskRegistration

interface RegistryResolver {
    suspend fun <I : Any, O : Any, P : Any> resolve(
        identifier: String
    ): TaskRegistration<I, O, P>?
}