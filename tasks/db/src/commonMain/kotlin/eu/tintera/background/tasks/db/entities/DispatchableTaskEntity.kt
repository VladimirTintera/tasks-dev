package eu.tintera.background.tasks.db.entities

import eu.tintera.background.tasks.db.StateDb
import kotlin.uuid.Uuid

data class DispatchableTaskEntity(
    val id: Uuid,
    val state: StateDb
)