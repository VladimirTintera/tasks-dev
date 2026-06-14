package eu.tintera.background.tasks

import kotlinx.serialization.protobuf.ProtoBuf

internal val protoBuf: ProtoBuf = ProtoBuf { encodeDefaults = true }