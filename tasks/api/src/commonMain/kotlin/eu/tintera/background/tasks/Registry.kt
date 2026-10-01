package eu.tintera.background.tasks

import eu.tintera.background.tasks.migrations.Migration
import eu.tintera.background.tasks.serialization.Serializer
import eu.tintera.background.tasks.serialization.TagSerializer
import kotlin.reflect.KClass

interface Registry {
    fun <Input : Any, Output : Any, Progress : Any> register(
        registration: TaskRegistration<Input, Output, Progress>,
    )

    fun <T : Tag> registerTag(
        identifier: String,
        type: KClass<out T>,
        serializer: TagSerializer<T>
    )

    /**
     * Declares that registrations are being made right now. Until the returned handle is closed,
     * looking up a handler or a tag that is not registered yet waits for it instead of giving up.
     *
     * Without a phase the registry only has its warmup window to go by: a time limit counted from
     * the first unanswered lookup. That is a guess, and it guesses wrong whenever registering
     * starts late — a background wake-up on a slow device can burn the whole window before the
     * application has registered anything — or when one eager singleton already uses the engine
     * while the registration of what it needs is still queued behind it. A phase replaces the
     * guess with a fact: whoever registers knows when it is done.
     *
     * Phases nest and may overlap, so each part of an application can open its own; lookups wait
     * until the last one is closed. Closing a handle twice is harmless. A phase that is never
     * closed keeps every lookup of a missing registration waiting forever, so prefer [registering],
     * which closes it even when registration throws. For the same reason, never block the
     * registering thread on a lookup made inside the phase — it would wait for itself.
     */
    fun openRegistrationPhase(): AutoCloseable
}

/** Runs [block] inside a registration phase, see [Registry.openRegistrationPhase]. */
inline fun <T> Registry.registering(block: () -> T): T = openRegistrationPhase().use { block() }

inline fun <reified T : Tag> Registry.registerTag(
    identifier: String,
    serializer: TagSerializer<T>
) = registerTag(
    identifier = identifier,
    type = T::class,
    serializer = serializer
)

inline fun <reified T : TaskHandler<I, O, P>, reified I : Any, reified O : Any, reified P : Any> Registry.register(
    identifier: String,
    currentVersion: Int = 1,
    inputSerializer: Serializer<I>,
    outputSerializer: Serializer<O>,
    progressSerializer: Serializer<P>,
    migrations: List<Migration> = emptyList(),
    noinline factory: () -> T
) = register(
    TaskRegistration(
        identifier = identifier,
        currentVersion = currentVersion,
        factory = factory,
        inputSerializer = inputSerializer,
        outputSerializer = outputSerializer,
        progressSerializer = progressSerializer,
        migrations = migrations,
        type = T::class
    )
)