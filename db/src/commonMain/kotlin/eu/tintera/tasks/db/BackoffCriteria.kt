package eu.tintera.tasks.db

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import kotlin.time.Duration

@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class BackoffCriteria(
    @ProtoNumber(1) val backoffPolicy: BackoffPolicy,
    @ProtoNumber(2) val delay: Duration
)