package eu.tintera.tasks.core.serialization

import eu.tintera.tasks.serialization.TaskDataSerializer

object UnitTaskDataSerializer : TaskDataSerializer<Unit> {
    override fun encodeToBytes(value: Unit): ByteArray {
        return byteArrayOf()
    }

    override fun decodeFromBytes(bytes: ByteArray) {

    }
}