package eu.tintera.tasks

import eu.tintera.tasks.serialization.Serializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer

private class ProtobufSerializer<T : Any>(
    private val kSerializer: KSerializer<T>
) : Serializer<T> {

    override fun encodeToBytes(value: T): ByteArray {
        return protoBuf.encodeToByteArray(kSerializer, value)
    }

    override fun decodeFromBytes(bytes: ByteArray): T {
        return protoBuf.decodeFromByteArray(kSerializer, bytes)
    }
}

fun <T : Any> protobufSerializer(
    serializer: KSerializer<T>
): Serializer<T> = ProtobufSerializer(serializer)

inline fun <reified T : Any> protobufSerializer(): Serializer<T> = protobufSerializer(serializer<T>())