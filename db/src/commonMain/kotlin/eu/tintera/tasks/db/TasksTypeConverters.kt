package eu.tintera.tasks.db

import androidx.room3.TypeConverter
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import kotlin.jvm.JvmStatic
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant
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
    fun toInstant(value: Long?): Instant? {
        return value?.let { Instant.fromEpochMilliseconds(it) }
    }

    @TypeConverter
    @JvmStatic
    fun fromInstant(value: Instant?): Long? {
        return value?.toEpochMilliseconds()
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
    fun toState(value: String?): StateDb? {
        return StateDb.entries.firstOrNull { it.name == value }
    }

    @TypeConverter
    @JvmStatic
    fun fromState(value: StateDb?): String? {
        return value?.name
    }

    @TypeConverter
    @JvmStatic
    fun toBackoffCriteria(value: ByteArray?): BackoffCriteriaDb? {
        return value?.let { ProtoBuf.decodeFromByteArray<BackoffCriteriaDb>(it) }
    }


    @TypeConverter
    @JvmStatic
    fun fromBackoffCriteria(value: BackoffCriteriaDb?): ByteArray? {
        return value?.let { ProtoBuf.encodeToByteArray<BackoffCriteriaDb>(it) }
    }
}