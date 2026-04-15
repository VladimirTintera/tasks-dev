package eu.tintera.tasks.core

import eu.tintera.tasks.TaskHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.KSerializer
import kotlin.time.Duration.Companion.seconds

class TaskRegistry {

    class TaskRegistration<Input, Output, Progress>(
        val factory: () -> TaskHandler<*, *, *>,
        val inputSerializer: KSerializer<Input>,
        val outputSerializer: KSerializer<Output>,
        val progressSerializer: KSerializer<Progress>,
    )

    private val registry = MutableStateFlow<Map<String, TaskRegistration<*, *, *>>>(emptyMap())

    fun <Input, Output, Progress> register(identifier: String, registration: TaskRegistration<Input, Output, Progress>) {
        registry.update { currentMap ->
            if (identifier in currentMap) {
                throw IllegalArgumentException("Handler for '$identifier' is already registered.")
            }
            currentMap + (identifier to registration)
        }
    }

    suspend fun resolve(
        identifier: String
    ): TaskRegistration<*,*,*>? = withTimeoutOrNull(5.seconds) {
        registry.first {
            it.containsKey(identifier)
        }[identifier]!!
    }
}