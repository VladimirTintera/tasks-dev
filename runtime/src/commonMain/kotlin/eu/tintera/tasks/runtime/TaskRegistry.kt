package eu.tintera.tasks.runtime

import eu.tintera.tasks.Registry
import eu.tintera.tasks.Tag
import eu.tintera.tasks.TaskRegistration
import eu.tintera.tasks.core.RegistryResolver
import eu.tintera.tasks.core.TagRegistration
import eu.tintera.tasks.core.migrations.findMigrationPath
import eu.tintera.tasks.serialization.TagSerializer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.reflect.KClass
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

internal val taskRegistry = TaskRegistry()

internal class TaskRegistry(
    private val clock: Clock = Clock.System
) : Registry, RegistryResolver {

    private val registry = MutableStateFlow<Map<String, TaskRegistration<out Any, out Any, out Any>>>(emptyMap())
    private val tagRegistry = MutableStateFlow<Map<String, TagRegistration<out Tag>>>(emptyMap())
    private val tagTypeRegistry = MutableStateFlow<Map<KClass<out Tag>, TagRegistration<out Tag>>>(emptyMap())

    private val mutex = Mutex()
    private val warmupTimeout = 5.seconds

    private data class Warmup(
        val startedAt: Instant? = null,
        val consumed: Boolean = false
    )

    private val warmupDone = MutableStateFlow(Warmup())

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
        registry.update { currentMap ->
            if (registration.identifier in currentMap) {
                throw IllegalArgumentException("Handler for '${registration.identifier}' is already registered.")
            }
            currentMap + (registration.identifier to registration)
        }
    }

    override fun <T : Tag> registerTag(
        identifier: String,
        type: KClass<out T>,
        serializer: TagSerializer<T>
    ) {
        val registration = TagRegistration(identifier = identifier, serializer = serializer)

        tagRegistry.update { currentMap ->
            if (identifier in currentMap) {
                throw IllegalArgumentException("Tag for $identifier is already registered.")
            }
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
    override suspend fun <T : Tag> resolveTag(
        identifier: String
    ): TagRegistration<T>? = tagRegistry.resolveWithWarmupCheck(identifier) as? TagRegistration<T>?

    @Suppress("UNCHECKED_CAST")
    override suspend fun <T : Tag> resolveTag(
        type: KClass<out T>
    ): TagRegistration<T>? = tagTypeRegistry.resolveWithWarmupCheck(type) as? TagRegistration<T>

    private suspend fun <T, R> MutableStateFlow<Map<T, R>>.resolveWithWarmupCheck(
        key: T
    ): R? {

        if (warmupDone.value.consumed) return value[key]

        val remainingWait = mutex.withLock {
            val now = clock.now()

            val updated = warmupDone.updateAndGet { current ->
                current.copy(startedAt = current.startedAt ?: now)
            }

            val elapsed = now - updated.startedAt!!
            warmupTimeout - elapsed
        }

        return if (remainingWait.isPositive()) {

            val value = withTimeoutOrNull(remainingWait) {
                first { it.containsKey(key) }[key]
            }

            if (value == null) warmupDone.update { it.copy(consumed = true) }
            value
        } else {

            warmupDone.update { it.copy(consumed = true) }

            value[key]
        }
    }
}