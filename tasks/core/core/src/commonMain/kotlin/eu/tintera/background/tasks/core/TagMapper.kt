package eu.tintera.background.tasks.core

import eu.tintera.background.tasks.LabelTag
import eu.tintera.background.tasks.Tag

class TagMapper(
    private val registryResolver: RegistryResolver
) {
    suspend fun parse(
        tags: Collection<String>
    ) = tags.mapNotNull { it.toTag() }.toSet()

    suspend fun serialize(
        tags: Collection<Tag>
    ): Set<String> = tags.map {
        when (it) {
            is LabelTag -> it.label
            else -> it.serialize()
        }
    }.toSet()

    private suspend fun Tag.serialize(): String {

        val registration = registryResolver.resolveTag<Tag>(
            type = this::class
        ) ?: error("Cannot resolve tag registration for '${this::class}'.")

        val serialized = registration.serializer.encodeToString(this)
        return "${PREFIX}${registration.identifier}$DELIMITER$serialized"
    }

    private suspend fun String.toTag(): Tag? = when {
        !startsWith(PREFIX) -> LabelTag(this)
        else -> {
            split(DELIMITER, limit = 3).takeIf {
                it.size == 3
            }?.let { parts ->
                registryResolver.resolveTag<Tag>(
                    identifier = parts[1]
                )?.serializer?.decodeFromStringOrNull(parts[2])
            } ?: LabelTag(this)
        }
    }

    companion object {
        private const val PREFIX = $$"$tt:"
        private const val DELIMITER = ":"
    }
}