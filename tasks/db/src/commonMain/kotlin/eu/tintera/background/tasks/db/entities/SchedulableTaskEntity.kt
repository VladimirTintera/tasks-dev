package eu.tintera.background.tasks.db.entities

import kotlin.time.Instant
import kotlin.uuid.Uuid

data class SchedulableTaskEntity(
    val id: Uuid,
    val processTime: Instant?,
    val requiresDeviceIdle: Boolean,
    val networkRequired: Boolean
)