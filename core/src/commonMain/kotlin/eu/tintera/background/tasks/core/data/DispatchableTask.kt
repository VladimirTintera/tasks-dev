package eu.tintera.background.tasks.core.data

import eu.tintera.background.tasks.State
import kotlin.uuid.Uuid

data class DispatchableTask(
    val id: Uuid,
    val state: State
)