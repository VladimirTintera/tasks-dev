package eu.tintera.background.tasks.runtime

import eu.tintera.background.tasks.Registry
import eu.tintera.background.tasks.Tag
import eu.tintera.background.tasks.TaskHandler
import eu.tintera.background.tasks.TaskRegistration
import eu.tintera.background.tasks.core.RegistryResolver
import eu.tintera.background.tasks.core.TagRegistration
import eu.tintera.background.tasks.core.migrations.findMigrationPath
import eu.tintera.background.tasks.serialization.TagSerializer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlin.reflect.KClass
import kotlin.time.Clock
import kotlin.time.Duration

internal val taskRegistry = TaskRegistry()

internal class TaskRegistry(
    clock: Clock = Clock.System,
    warmupTimeout: Duration = DEFAULT_WARMUP_TIMEOUT
) : Registry, RegistryResolver, WarmupCache(
    clock = clock,
    warmupTimeout = warmupTimeout
) {
    private val registry = MutableStateFlow<Map<String, TaskRegistration<out Any, out Any, out Any>>>(emptyMap())
    private val typeRegistry =
        MutableStateFlow<Map<KClass<out TaskHandler<*, *, *>>, List<TaskRegistration<*, *, *>>>>(emptyMap())
    private val tagRegistry = MutableStateFlow<Map<String, TagRegistration<out Tag>>>(emptyMap())
    private val tagTypeRegistry = MutableStateFlow<Map<KClass<out Tag>, TagRegistration<out Tag>>>(emptyMap())

    override fun <Input : Any, Output : Any, Progress : Any> register(
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
        // Opakovaná registrace téhož handleru NENÍ chyba: registrace přichází z Koinu konzumenta
        // (`taskHandlerOf` = `createdAtStart` singleton), kdežto tenhle registr je procesový
        // singleton, který restart Koinu přežije. Aplikace, která svůj Koin restartuje (odhlášení,
        // přepnutí uživatele), tedy pošle tytéž registrace znovu — a shodit ji za to by bylo hrubé.
        // Chyba je až kolize dvou RŮZNÝCH handlerů na jednom identifikátoru.
        registry.value[registration.identifier]?.let { existing ->
            require(existing.type == registration.type) {
                "Identifier '${registration.identifier}' is already registered for " +
                    "'${existing.type.simpleName}' and cannot be reused for '${registration.type.simpleName}'."
            }
        }

        registry.update { currentMap ->
            currentMap + (registration.identifier to registration)
        }

        typeRegistry.update { currentMap ->
            val forType = currentMap.getOrElse(registration.type) { emptyList() }
                // Bez tohohle by se při každém restartu Koinu seznam pro daný typ prodloužil.
                .filterNot { it.identifier == registration.identifier }

            currentMap + (registration.type to (forType + registration))
        }
    }

    override fun <T : Tag> registerTag(
        identifier: String,
        type: KClass<out T>,
        serializer: TagSerializer<T>
    ) {
        val registration = TagRegistration(identifier = identifier, serializer = serializer)

        // Stejná úvaha jako u register(): opakování při restartu Koinu je legitimní, kolize dvou
        // různých typů na jednom identifikátoru je chyba.
        tagTypeRegistry.value.entries.firstOrNull { it.value.identifier == identifier }?.let { existing ->
            require(existing.key == type) {
                "Tag identifier '$identifier' is already registered for " +
                    "'${existing.key.simpleName}' and cannot be reused for '${type.simpleName}'."
            }
        }

        tagRegistry.update { currentMap ->
            currentMap + (identifier to registration)
        }

        tagTypeRegistry.update { currenMap ->
            currenMap + (type to registration)
        }
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun <I : Any, O : Any, P : Any> resolve(
        identifier: String
    ): TaskRegistration<I, O, P>? = registry.resolveWithWarmupCheck(identifier) as? TaskRegistration<I, O, P>

    @Suppress("UNCHECKED_CAST")
    override suspend fun <T : TaskHandler<I, O, P>, I : Any, O : Any, P : Any> resolve(
        type: KClass<out T>
    ): List<TaskRegistration<I, O, P>>? = typeRegistry.resolveWithWarmupCheck(type) as? List<TaskRegistration<I, O, P>>

    @Suppress("UNCHECKED_CAST")
    override suspend fun <T : Tag> resolveTag(
        identifier: String
    ): TagRegistration<T>? = tagRegistry.resolveWithWarmupCheck(identifier) as? TagRegistration<T>?

    @Suppress("UNCHECKED_CAST")
    override suspend fun <T : Tag> resolveTag(
        type: KClass<out T>
    ): TagRegistration<T>? = tagTypeRegistry.resolveWithWarmupCheck(type) as? TagRegistration<T>
}