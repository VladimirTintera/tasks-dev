package eu.tintera.background.tasks.core.serialization

import eu.tintera.background.tasks.serialization.Serializer

object UnitSerializer : Serializer<Unit> {
    override fun encodeToBytes(value: Unit): ByteArray {
        return byteArrayOf()
    }

    override fun decodeFromBytes(bytes: ByteArray) {

    }
}