package eu.tintera.tasks.serialization

import eu.tintera.tasks.Tag

interface TagSerializer<T: Tag> {
    fun encodeToString(value: T) : String
    fun decodeFromStringOrNull(value: String): T?
}