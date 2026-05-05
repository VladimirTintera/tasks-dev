package eu.tintera.tasks

class Tags(
    @PublishedApi internal val rawTags: Set<String> = emptySet(),
    @PublishedApi internal val typedTags: Set<Tag> = emptySet()
) {
    operator fun contains(tag: String): Boolean = rawTags.contains(tag)

    inline fun <reified T : Tag> containsType(): Boolean = typedTags.any { it is T }

    inline fun <reified T : Tag> get(): List<T> = typedTags.filterIsInstance<T>()

    fun rawTags(): List<String> = rawTags.toList()

    inline fun <reified T : Tag> getOrNull(): T? = get<T>().firstOrNull()

    operator fun contains(tag: Tag): Boolean = typedTags.contains(tag)

    override fun toString(): String = "Tags(raw=$rawTags, typed=$typedTags)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Tags

        if (rawTags != other.rawTags) return false
        if (typedTags != other.typedTags) return false

        return true
    }

    override fun hashCode(): Int {
        var result = rawTags.hashCode()
        result = 31 * result + typedTags.hashCode()
        return result
    }
}