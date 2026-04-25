package eu.tintera.tasks

import eu.tintera.tasks.serialization.TagSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import kotlin.io.encoding.Base64

private class ProtobufTagSerializer<T : Tag>(
    private val kSerializer: KSerializer<T>
) : TagSerializer<T> {

    override fun encodeToString(value: T): String {
        return Base64.encode(protoBuf.encodeToByteArray(kSerializer, value))
    }

    override fun decodeFromStringOrNull(
        value: String
    ): T? = try {
        protoBuf.decodeFromByteArray(kSerializer, Base64.decode(value))
    } catch (_: Throwable) {
        null
    }
}

fun <T : Tag> protobufTagSerializer(serializer: KSerializer<T>): TagSerializer<T> = ProtobufTagSerializer(serializer)
inline fun <reified T : Tag> protobufTagSerializer(): TagSerializer<T> = protobufTagSerializer(serializer<T>())