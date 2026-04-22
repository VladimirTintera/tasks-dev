package eu.tintera.tasks

import eu.tintera.tasks.serialization.TaskDataSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.serializer

private val protoBuf: ProtoBuf = ProtoBuf { encodeDefaults = true }

private class ProtobufTaskDataSerializer<T : Any>(
    private val kSerializer: KSerializer<T>
) : TaskDataSerializer<T> {

    override fun encodeToBytes(value: T): ByteArray {
        return protoBuf.encodeToByteArray(kSerializer, value)
    }

    override fun decodeFromBytes(bytes: ByteArray): T {
        return protoBuf.decodeFromByteArray(kSerializer, bytes)
    }
}

fun <T : Any> protobufSerializer(serializer: KSerializer<T>): TaskDataSerializer<T> =
    ProtobufTaskDataSerializer(serializer)

inline fun <reified T : Any> protobufSerializer(): TaskDataSerializer<T> = protobufSerializer(serializer<T>())