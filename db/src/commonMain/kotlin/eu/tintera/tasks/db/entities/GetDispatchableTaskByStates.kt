package eu.tintera.tasks.db.entities

import eu.tintera.tasks.db.State
import kotlin.uuid.Uuid

internal data class GetDispatchableTaskByStates(
    val id: Uuid,
    val state: State
)