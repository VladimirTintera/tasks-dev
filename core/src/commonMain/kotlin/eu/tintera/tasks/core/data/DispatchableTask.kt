package eu.tintera.tasks.core.data

import eu.tintera.tasks.State
import kotlin.uuid.Uuid

data class DispatchableTask(
    val id: Uuid,
    val state: State
)