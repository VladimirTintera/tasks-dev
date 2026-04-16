package eu.tintera.tasks.serialization

interface TaskDataSerializer<T> {
    fun encodeToBytes(value: T): ByteArray
    fun decodeFromBytes(bytes: ByteArray): T
}