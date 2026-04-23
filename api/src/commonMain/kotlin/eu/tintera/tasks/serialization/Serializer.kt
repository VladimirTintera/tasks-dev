package eu.tintera.tasks.serialization

interface Serializer<T> {
    fun encodeToBytes(value: T): ByteArray
    fun decodeFromBytes(bytes: ByteArray): T
}