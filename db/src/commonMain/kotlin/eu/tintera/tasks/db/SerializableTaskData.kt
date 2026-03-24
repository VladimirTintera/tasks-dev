package eu.tintera.tasks.db

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class SerializableValue(
    @ProtoNumber(1) val key: String,
    @ProtoNumber(2) val intValue: Int?,
    @ProtoNumber(3) val stringValue: String?,
    @ProtoNumber(4) val longValue: Long?,
    @ProtoNumber(5) val booleanValue: Boolean?
)
@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class SerializableTaskData(
    @ProtoNumber(1) val values: List<SerializableValue>
)