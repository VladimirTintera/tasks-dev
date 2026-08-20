package eu.tintera.background.tasks.koin

import eu.tintera.background.tasks.Tag
import eu.tintera.background.tasks.Tasks
import eu.tintera.background.tasks.serialization.TagSerializer
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import kotlin.reflect.KClass


@PublishedApi
internal class TagRegistration<T : Tag>(
    val identifier: String,
    val serializer: TagSerializer<T>,
    val type: KClass<T>
) {
    init {
        Tasks.registry.registerTag(
            identifier = identifier,
            type = type,
            serializer = serializer
        )
    }
}

/**
 * Registers a tag type so [eu.tintera.background.tasks.core.TagMapper] can serialize it and read it
 * back.
 *
 * The qualifier is not optional. Koin erases generics, so every `TagRegistration<*>` shares one key:
 * without `named<T>()` the second registered tag type silently replaces the first, and only the last
 * one ever reaches the registry. The failure surfaces far away as
 * `Cannot resolve tag registration for '<some other tag>'`.
 */
inline fun <reified T : Tag> Module.taskTag(
    identifier: String,
    serializer: TagSerializer<T>
) {
    single(named<T>(), createdAtStart = true) {
        TagRegistration(
            identifier = identifier,
            serializer = serializer,
            type = T::class
        )
    }
}