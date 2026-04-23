package eu.tintera.tasks.core.serialization

import eu.tintera.tasks.serialization.Serializer

object UnitSerializer : Serializer<Unit> {
    override fun encodeToBytes(value: Unit): ByteArray {
        return byteArrayOf()
    }

    override fun decodeFromBytes(bytes: ByteArray) {

    }
}