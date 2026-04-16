package eu.tintera.tasks.core

import eu.tintera.tasks.core.seriaization.SerializationEngine
import kotlinx.serialization.KSerializer

@Suppress("UNCHECKED_CAST")
internal fun <T> ByteArray?.toTypedData(
    serializationEngine: SerializationEngine,
    serializer: KSerializer<T>
): T = this?.let {
    serializationEngine.decodeFromBytes(it, serializer)
} ?: when {
    serializer.descriptor.serialName == "kotlin.Unit" -> Unit as T
    serializer.descriptor.isNullable -> null as T
    else -> error("Required data missing!")
}