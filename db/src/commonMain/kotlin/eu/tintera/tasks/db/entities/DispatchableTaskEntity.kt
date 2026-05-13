package eu.tintera.tasks.db.entities

import eu.tintera.tasks.db.StateDb
import kotlin.uuid.Uuid

data class DispatchableTaskEntity(
    val id: Uuid,
    val state: StateDb
)