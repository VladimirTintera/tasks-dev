package eu.tintera.background.tasks

import eu.tintera.background.tasks.compat.Data
import eu.tintera.background.tasks.compat.taskDataOf
import eu.tintera.background.tasks.serialization.Serializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.protobuf.ProtoNumber

/**
 * Serializer for the untyped [Data] payload of the `compat` module.
 *
 * The wire format is an **internal detail**. It is written and read only by this library, never
 * exchanged with anything else, so the surrogate below can change shape whenever a schema migration
 * accompanies it. Do not rely on the bytes.
 */
fun dataSerializer(): Serializer<Data> = DataSerializer

private object DataSerializer : Serializer<Data> {

    private val protoBuf: ProtoBuf = ProtoBuf { encodeDefaults = true }

    override fun encodeToBytes(value: Data): ByteArray =
        protoBuf.encodeToByteArray(DataSurrogate.serializer(), value.toSurrogate())

    override fun decodeFromBytes(bytes: ByteArray): Data =
        protoBuf.decodeFromByteArray(DataSurrogate.serializer(), bytes).toData()
}

private fun DataSurrogate.toData() = taskDataOf(
    *entries.flatMap { entry ->
        listOfNotNull(
            entry.intValue?.let { entry.key to it },
            entry.stringValue?.let { entry.key to it },
            entry.booleanValue?.let { entry.key to it },
            entry.longValue?.let { entry.key to it }
        )
    }.toTypedArray()
)

private fun Data.toSurrogate() = DataSurrogate(
    entries = map.map { (key, _) ->
        DataEntry(
            key = key,
            intValue = getInt(key),
            stringValue = getString(key),
            longValue = getLong(key),
            booleanValue = getBoolean(key)
        )
    }
)

/**
 * A [Data] entry as protobuf sees it. Exactly one of the value fields is set — [Data] only holds
 * `Int`, `String`, `Long` and `Boolean`.
 */
@Serializable
private data class DataEntry(
    @ProtoNumber(1) val key: String,
    @ProtoNumber(2) val intValue: Int?,
    @ProtoNumber(3) val stringValue: String?,
    @ProtoNumber(4) val longValue: Long?,
    @ProtoNumber(5) val booleanValue: Boolean?
)

@Serializable
private data class DataSurrogate(
    @ProtoNumber(1) val entries: List<DataEntry>
)
