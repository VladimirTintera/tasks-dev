package eu.tintera.tasks.core.fakes

import eu.tintera.tasks.serialization.TaskDataSerializer

class FakeStringSerializer<T>(
    private val encodeLogic: (T) -> String = { it.toString() },
    private val decodeLogic: (String) -> T
) : TaskDataSerializer<T> {

    override fun encodeToBytes(value: T): ByteArray {
        return encodeLogic(value).encodeToByteArray()
    }

    override fun decodeFromBytes(bytes: ByteArray): T {
        return decodeLogic(bytes.decodeToString())
    }
}