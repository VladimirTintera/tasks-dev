package eu.tintera.tasks

import eu.tintera.tasks.serialization.TaskDataSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

private val json: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
}

private class JsonTaskDataSerializer<T : Any>(
    private val kSerializer: KSerializer<T>
) : TaskDataSerializer<T> {

    override fun encodeToBytes(value: T): ByteArray {
        return json.encodeToString(kSerializer, value).encodeToByteArray()
    }

    override fun decodeFromBytes(bytes: ByteArray): T {
        return json.decodeFromString(kSerializer, bytes.decodeToString())
    }
}

fun <T : Any> jsonSerializer(serializer: KSerializer<T>): TaskDataSerializer<T> = JsonTaskDataSerializer(serializer)

inline fun <reified T : Any> jsonSerializer(): TaskDataSerializer<T> = jsonSerializer(serializer<T>())