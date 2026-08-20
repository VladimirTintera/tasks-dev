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
    ): Set<String> = tags.map { serialize(it) }.toSet()

    /**
     * The wire form of a single tag. A [LabelTag] is its own label, so a typed lookup and a raw
     * string lookup for the same label find the same tasks.
     */
    suspend fun serialize(
        tag: Tag
    ): String = when (tag) {
        is LabelTag -> tag.label
        else -> tag.serializeTyped()
    }

    private suspend fun Tag.serializeTyped(): String {

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