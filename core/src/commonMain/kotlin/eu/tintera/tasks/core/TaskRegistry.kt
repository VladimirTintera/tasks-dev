package eu.tintera.tasks.core

import eu.tintera.tasks.TaskHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

class TaskRegistry {

    private val registry = MutableStateFlow<Map<String, () -> TaskHandler>>(emptyMap())

    fun register(identifier: String, factory: () -> TaskHandler) {
        registry.update { currentMap ->
            if (identifier in currentMap) {
                throw IllegalArgumentException("Handler for '$identifier' is already registered.")
            }
            currentMap + (identifier to factory)
        }
    }

    suspend fun resolve(
        identifier: String
    ): TaskHandler? = withTimeoutOrNull(5.seconds) {
        registry.first {
            it.containsKey(identifier)
        }[identifier]!!.invoke()
    }
}