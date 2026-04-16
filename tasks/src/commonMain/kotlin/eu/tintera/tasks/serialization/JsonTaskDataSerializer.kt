package eu.tintera.tasks.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

private val json: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
}
private class JsonTaskDataSerializer<T>(
    private val kSerializer: KSerializer<T>
) : TaskDataSerializer<T> {

    override fun encodeToBytes(value: T): ByteArray {
        if (value == null) return byteArrayOf() // Pokud podporuješ null
        return json.encodeToString(kSerializer, value).encodeToByteArray()
    }

    override fun decodeFromBytes(bytes: ByteArray): T {
        return json.decodeFromString(kSerializer, bytes.decodeToString())
    }
}

fun <T> jsonSerializer(serializer: KSerializer<T>): TaskDataSerializer<T> = JsonTaskDataSerializer(serializer)

inline fun <reified T> jsonSerializer(): TaskDataSerializer<T> = jsonSerializer(serializer<T>())