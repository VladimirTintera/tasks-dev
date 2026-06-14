package eu.tintera.background.tasks.serialization

import eu.tintera.background.tasks.Tag

interface TagSerializer<T: Tag> {
    fun encodeToString(value: T) : String
    fun decodeFromStringOrNull(value: String): T?
}