package eu.tintera.background.tasks.android

import eu.tintera.background.tasks.State
import kotlin.uuid.Uuid

data class TaskWithState(
    val id: Uuid,
    val state: State
)