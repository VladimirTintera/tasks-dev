package eu.tintera.tasks.db

import androidx.room.TypeConverter
import kotlin.time.Instant
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import kotlin.jvm.JvmStatic
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.uuid.Uuid

@OptIn(ExperimentalSerializationApi::class)
internal object TasksTypeConverters {

    @TypeConverter
    @JvmStatic
    fun toUuid(value: String?): Uuid? {
        return value?.let { Uuid.parse(it) }
    }

    @TypeConverter
    @JvmStatic
    fun fromUuid(value: Uuid?): String? {
        return value?.toString()
    }

    @TypeConverter
    @JvmStatic
    fun toInstant(value: String?): Instant? {
        return value?.let { Instant.parse(it) }
    }

    @TypeConverter
    @JvmStatic
    fun fromInstant(value: Instant?): String? {
        return value?.toString()
    }


    @TypeConverter
    @JvmStatic
    fun toDuration(value: Long?): Duration? {
        return value?.milliseconds
    }

    @TypeConverter
    @JvmStatic
    fun fromDuration(value: Duration?): Long? {
        return value?.inWholeMilliseconds
    }


    @TypeConverter
    @JvmStatic
    fun toState(value: String?): State? {
        return State.entries.firstOrNull { it.name == value }
    }

    @TypeConverter
    @JvmStatic
    fun fromState(value: State?): String? {
        return value?.name
    }


    @TypeConverter
    @JvmStatic
    fun toSerializableTaskData(value: ByteArray?): SerializableTaskData? {
        return value?.let {
            ProtoBuf.decodeFromByteArray<SerializableTaskData>(it)
        }
    }

    @TypeConverter
    @JvmStatic
    fun fromSerializableTaskData(value: SerializableTaskData?): ByteArray? {
        return value?.let {
             ProtoBuf.encodeToByteArray<SerializableTaskData>(it)
        }
    }

    @TypeConverter
    @JvmStatic
    fun toBackoffCriteria(value: ByteArray?): BackoffCriteria? {
        return value?.let { ProtoBuf.decodeFromByteArray<BackoffCriteria>(it) }
    }


    @TypeConverter
    @JvmStatic
    fun fromBackoffCriteria(value: BackoffCriteria?): ByteArray? {
        return value?.let { ProtoBuf.encodeToByteArray<BackoffCriteria>(it) }
    }
}