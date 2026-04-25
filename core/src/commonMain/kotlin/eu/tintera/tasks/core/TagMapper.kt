package eu.tintera.tasks.core

import eu.tintera.tasks.Tag

data class TaskTags(
    val tags: Set<String>,
    val typedTags: Set<Tag>
)

class TagMapper(
    private val registryResolver: RegistryResolver
) {
    suspend fun parse(
        tags: Collection<String>
    ) = tags.fold(
        Pair(mutableSetOf<String>(), mutableSetOf<Tag>())
    ) { acc, tag ->
        when(val parsed = tag.toTag()) {
            ParsedTag.Invalid -> {}
            is ParsedTag.Label -> acc.first.add(parsed.value)
            is ParsedTag.Valid -> acc.second.add(parsed.tag)
        }
        acc
    }.let { (tags, typedTags) ->
        TaskTags(
            tags = tags,
            typedTags = typedTags
        )
    }

    suspend fun serialize(
        tags: TaskTags
    ): Set<String> = tags.tags + tags.typedTags.map { it.serialize() }

    private suspend fun Tag.serialize(): String {

        val registration = registryResolver.resolveTag<Tag>(
            type = this::class
        ) ?: error("Cannot resolve tag registration for '${this::class}'.")

        val serialized = registration.serializer.encodeToString(this)
        return "${PREFIX}${registration.identifier}$DELIMITER$serialized"
    }

    private sealed interface ParsedTag {
        data class Valid(val tag: Tag) : ParsedTag
        data object Invalid : ParsedTag

        data class Label(val value: String) : ParsedTag
    }
    private suspend fun String.toTag(): ParsedTag = when {
        !startsWith(PREFIX) -> ParsedTag.Label(this)
        else -> {
            split(DELIMITER, limit = 3).takeIf {
                it.size == 3
            }?.let { parts ->
                registryResolver.resolveTag<Tag>(
                    identifier = parts[1]
                )?.serializer?.decodeFromStringOrNull(parts[2])?.let {
                    ParsedTag.Valid(it)
                } ?: ParsedTag.Invalid
            } ?: ParsedTag.Label(this)
        }
    }

    companion object {
        private const val PREFIX = $$"$tt:"
        private const val DELIMITER = ":"
    }
}