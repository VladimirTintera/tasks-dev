package eu.tintera.tasks.db

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import kotlin.time.Duration

@Serializable
data class BackoffCriteriaDb(
    @ProtoNumber(1) val backoffPolicy: BackoffPolicyDb,
    @ProtoNumber(2) val delay: Duration
)