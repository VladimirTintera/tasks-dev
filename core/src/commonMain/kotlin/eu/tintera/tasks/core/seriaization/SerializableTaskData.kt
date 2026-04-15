package eu.tintera.tasks.core.seriaization

import eu.tintera.tasks.Data
import eu.tintera.tasks.taskDataOf
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import kotlin.collections.component1
import kotlin.collections.component2

@Serializable
internal data class SerializableValue(
    @ProtoNumber(1) val key: String,
    @ProtoNumber(2) val intValue: Int?,
    @ProtoNumber(3) val stringValue: String?,
    @ProtoNumber(4) val longValue: Long?,
    @ProtoNumber(5) val booleanValue: Boolean?
)
@Serializable
internal data class SerializableTaskData(
    @ProtoNumber(1) val values: List<SerializableValue>
)


