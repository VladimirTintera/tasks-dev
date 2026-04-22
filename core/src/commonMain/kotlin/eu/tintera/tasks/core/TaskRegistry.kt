package eu.tintera.tasks.core

import eu.tintera.tasks.TaskHandler
import eu.tintera.tasks.core.migrations.findMigrationPath
import eu.tintera.tasks.migrations.Migration
import eu.tintera.tasks.serialization.TaskDataSerializer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

class TaskRegistry {

    class TaskRegistration<Input : Any, Output : Any, Progress : Any>(
        val currentVersion: Int,
        val factory: () -> TaskHandler<Input, Output, Progress>,
        val inputSerializer: TaskDataSerializer<Input>,
        val outputSerializer: TaskDataSerializer<Output>,
        val progressSerializer: TaskDataSerializer<Progress>,
        val migrations: List<Migration>
    ) {
        init {
            // FAIL-FAST VALIDACE MIGRACÍ
            if (currentVersion > 1) {
                // Zkusíme nasimulovat cestu z každé historické verze (od 1 až po currentVersion - 1)
                for (startVer in 1 until currentVersion) {
                    try {
                        migrations.findMigrationPath(
                            startVersion = startVer,
                            targetVersion = currentVersion,
                        )
                    } catch (e: IllegalStateException) {
                        // Algoritmus cestu nenašel! Aplikaci okamžitě a nekompromisně shodíme
                        // s krásnou chybovou hláškou, která vývojáři přesně řekne, co udělal špatně.
                        throw IllegalArgumentException(
                            "🚨 Fatální chyba registrace úkolu!\n" +
                                    "Handler vyžaduje verzi $currentVersion, ale framework nenašel " +
                                    "migrační cestu z historické verze $startVer.\n" +
                                    "Doplň chybějící `migration(...)` do registrace, jinak by staré úkoly v databázi selhaly.",
                            e
                        )
                    }
                }
            }
        }
    }

    private val registry = MutableStateFlow<Map<String, TaskRegistration<*, *, *>>>(emptyMap())

    fun <Input : Any, Output : Any, Progress : Any> register(
        identifier: String,
        registration: TaskRegistration<Input, Output, Progress>
    ) {
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