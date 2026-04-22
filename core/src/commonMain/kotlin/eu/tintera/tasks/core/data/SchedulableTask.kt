package eu.tintera.tasks.core.data

import kotlin.time.Instant
import kotlin.uuid.Uuid

data class SchedulableTask(
    val id: Uuid,
    val processTime: Instant?,
    val requiresDeviceIdle: Boolean,
    val networkRequired: Boolean
)