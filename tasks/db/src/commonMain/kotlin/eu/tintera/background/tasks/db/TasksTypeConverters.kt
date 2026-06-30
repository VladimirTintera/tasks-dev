package eu.tintera.background.tasks.db

import androidx.room3.ColumnTypeConverter
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

    @ColumnTypeConverter
    @JvmStatic
    fun toUuid(value: String?): Uuid? {
        return value?.let { Uuid.parse(it) }
    }

    @ColumnTypeConverter
    @JvmStatic
    fun fromUuid(value: Uuid?): String? {
        return value?.toString()
    }

    @ColumnTypeConverter
    @JvmStatic
    fun toInstant(value: Long?): Instant? {
        return value?.let { Instant.fromEpochMilliseconds(it) }
    }

    @ColumnTypeConverter
    @JvmStatic
    fun fromInstant(value: Instant?): Long? {
        return value?.toEpochMilliseconds()
    }


    @ColumnTypeConverter
    @JvmStatic
    fun toDuration(value: Long?): Duration? {
        return value?.milliseconds
    }

    @ColumnTypeConverter
    @JvmStatic
    fun fromDuration(value: Duration?): Long? {
        return value?.inWholeMilliseconds
    }


    @ColumnTypeConverter
    @JvmStatic
    fun toState(value: String?): StateDb? {
        return StateDb.entries.firstOrNull { it.name == value }
    }

    @ColumnTypeConverter
    @JvmStatic
    fun fromState(value: StateDb?): String? {
        return value?.name
    }

    @ColumnTypeConverter
    @JvmStatic
    fun toBackoffCriteria(value: ByteArray?): BackoffCriteriaDb? {
        return value?.let { ProtoBuf.decodeFromByteArray<BackoffCriteriaDb>(it) }
    }


    @ColumnTypeConverter
    @JvmStatic
    fun fromBackoffCriteria(value: BackoffCriteriaDb?): ByteArray? {
        return value?.let { ProtoBuf.encodeToByteArray<BackoffCriteriaDb>(it) }
    }
}