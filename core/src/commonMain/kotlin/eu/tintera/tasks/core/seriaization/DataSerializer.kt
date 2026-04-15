package eu.tintera.tasks.core.seriaization

import eu.tintera.tasks.Data
import eu.tintera.tasks.taskDataOf
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder


object DataSerializer : KSerializer<Data> {
    override val descriptor = buildClassSerialDescriptor(DataSerializer::class.qualifiedName ?: "DataSerializer")

    override fun serialize(encoder: Encoder, value: Data) {
        error("Toto by se nemělo nikdy zavolat. SerializationEngine to má odchytit!")
    }

    override fun deserialize(decoder: Decoder): Data {
        error("Toto by se nemělo nikdy zavolat. SerializationEngine to má odchytit!")
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