package eu.tintera.tasks.core

import eu.tintera.tasks.TaskRegistration
import eu.tintera.tasks.core.migrations.findMigrationPath
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

class TaskRegistry {

    private val registry = MutableStateFlow<Map<String, TaskRegistration<out Any, out Any, out Any>>>(emptyMap())

    fun <Input : Any, Output : Any, Progress : Any> register(
        identifier: String,
        registration: TaskRegistration<Input, Output, Progress>
    ) {
        if (registration.currentVersion > 1) {
            // Zkusíme nasimulovat cestu z každé historické verze (od 1 až po currentVersion - 1)
            for (startVer in 1 until registration.currentVersion) {
                try {
                    registration.migrations.findMigrationPath(
                        startVersion = startVer,
                        targetVersion = registration.currentVersion,
                    )
                } catch (e: IllegalStateException) {
                    throw IllegalArgumentException(
                        "Fatal registration exception! Handler '${registration.identifier}' requires version $registration.currentVersion, but no migration path from $startVer found",
                        e
                    )
                }
            }
        }
        registry.update { currentMap ->
            if (identifier in currentMap) {
                throw IllegalArgumentException("Handler for '$identifier' is already registered.")
            }
            currentMap + (identifier to registration)
        }
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun <I : Any, O : Any, P : Any> resolve(
        identifier: String
    ): TaskRegistration<I, O, P>? = withTimeoutOrNull(5.seconds) {
        registry.first {
            it.containsKey(identifier)
        }[identifier]!! as TaskRegistration<I, O, P>
    }
}