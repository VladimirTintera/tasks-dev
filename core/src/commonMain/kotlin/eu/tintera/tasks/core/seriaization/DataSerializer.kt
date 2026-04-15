package eu.tintera.tasks.core.seriaization

import eu.tintera.tasks.Data
import eu.tintera.tasks.taskDataOf
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

internal object DataSerializer : KSerializer<Data> {

    override val descriptor: SerialDescriptor = SerializableTaskData.serializer().descriptor

    override fun serialize(encoder: Encoder, value: Data) {
        val surrogate = value.toSerializableTaskData()
        encoder.encodeSerializableValue(SerializableTaskData.serializer(), surrogate)
    }

    override fun deserialize(decoder: Decoder): Data {
        val surrogate = decoder.decodeSerializableValue(SerializableTaskData.serializer())
        return surrogate.toData()
    }
}

internal fun SerializableTaskData.toData() = taskDataOf(
    *values.flatMap { value ->
        listOfNotNull(
            value.intValue?.let { value.key to it },
            value.stringValue?.let { value.key to it },
            value.booleanValue?.let { value.key to it },
            value.longValue?.let { value.key to it }
        )
    }.toTypedArray()
)

internal fun Data.toSerializableTaskData() = SerializableTaskData(
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