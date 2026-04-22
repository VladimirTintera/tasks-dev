package eu.tintera.tasks.db.entities

import kotlin.time.Instant
import kotlin.uuid.Uuid

internal data class SchedulableTaskEntity(
    val id: Uuid,
    val processTime: Instant?,
    val requiresDeviceIdle: Boolean,
    val networkRequired: Boolean
)