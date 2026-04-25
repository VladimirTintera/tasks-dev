package eu.tintera.tasks

import kotlinx.serialization.protobuf.ProtoBuf

internal val protoBuf: ProtoBuf = ProtoBuf { encodeDefaults = true }