package eu.tintera.tasks.android

import eu.tintera.tasks.State
import kotlin.uuid.Uuid

data class TaskWithState(
    val id: Uuid,
    val state: State
)