package eu.tintera.tasks

interface Tag

data class LabelTag(
    val label: String
) : Tag

class TagsBuilder {
    private val tags = mutableSetOf<Tag>()

    fun <T : Tag> tag(tag: T) {
        tags.add(tag)
    }

    fun label(value: String) {
        tags.add(LabelTag(value))
    }

    fun build(): Set<Tag> = tags.toSet()
}

fun tags(
    block: TagsBuilder.() -> Unit
): Set<Tag> = TagsBuilder().apply { block() }.build()


operator fun Set<Tag>.contains(tag: String): Boolean = contains(LabelTag(tag))

inline fun <reified T : Tag> Set<Tag>.containsType(): Boolean = any { it is T }

inline fun <reified T : Tag> Set<Tag>.get(): List<T> = filterIsInstance<T>()

fun Set<Tag>.labels(): List<String> = filterIsInstance<LabelTag>().map { it.label }

inline fun <reified T : Tag> Set<Tag>.getOrNull(): T? = get<T>().firstOrNull()