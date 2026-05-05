package eu.tintera.tasks

data class TagsRequest(
    val rawTags: Set<String> = emptySet(),
    val tags: Set<Tag> = emptySet(),
)

operator fun TagsRequest.plus(other: TagsRequest) = TagsRequest(
    rawTags = rawTags + other.rawTags,
    tags = tags + other.tags
)

class TagsRequestBuilder {
    private var rawTags = mutableSetOf<String>()
    private var typedTags = mutableSetOf<Tag>()

    fun <T : Tag> tag(tag: T) {
        typedTags.add(tag)
    }

    fun tag(tag: String) {
        rawTags.add(tag)
    }

    fun build(): TagsRequest = TagsRequest(
        rawTags = rawTags,
        tags = typedTags
    )
}

fun tags(
    block: TagsRequestBuilder.() -> Unit
): TagsRequest = TagsRequestBuilder().apply { block() }.build()