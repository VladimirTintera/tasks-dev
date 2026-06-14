package eu.tintera.background.tasks

import eu.tintera.background.tasks.compat.Data
import eu.tintera.background.tasks.compat.taskDataOf
import eu.tintera.background.tasks.serialization.Serializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.protobuf.ProtoNumber

private object LegacySerializer : Serializer<Data> {

    private val protoBuf: ProtoBuf = ProtoBuf { encodeDefaults = true }

    override fun encodeToBytes(value: Data): ByteArray {
        val surrogate = value.toSerializableTaskData()
        return protoBuf.encodeToByteArray(SerializableTaskData.serializer(), surrogate)
    }

    override fun decodeFromBytes(bytes: ByteArray): Data {
        val surrogate = protoBuf.decodeFromByteArray(SerializableTaskData.serializer(), bytes)
        return surrogate.toByteArray()
    }
}

fun legacySerializer(): Serializer<Data> = LegacySerializer

private fun SerializableTaskData.toByteArray() = taskDataOf(
    *values.flatMap { value ->
        listOfNotNull(
            value.intValue?.let { value.key to it },
            value.stringValue?.let { value.key to it },
            value.booleanValue?.let { value.key to it },
            value.longValue?.let { value.key to it }
        )
    }.toTypedArray()
)

private fun Data.toSerializableTaskData() = SerializableTaskData(
    values = map.map { (key, _) ->
        SerializableValue(
            key = key,
            intValue = getInt(key),
            stringValue = getString(key),
            longValue = getLong(key),
            booleanValue = getBoolean(key)
        )
    }
)

@Serializable
private data class SerializableValue(
    @ProtoNumber(1) val key: String,
    @ProtoNumber(2) val intValue: Int?,
    @ProtoNumber(3) val stringValue: String?,
    @ProtoNumber(4) val longValue: Long?,
    @ProtoNumber(5) val booleanValue: Boolean?
)

@Serializable
private data class SerializableTaskData(
    @ProtoNumber(1) val values: List<SerializableValue>
)
