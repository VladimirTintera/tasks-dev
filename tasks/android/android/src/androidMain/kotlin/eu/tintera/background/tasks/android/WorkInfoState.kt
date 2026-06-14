package eu.tintera.background.tasks.android

import eu.tintera.background.tasks.State
import kotlin.uuid.Uuid

data class WorkInfoState(
    val id: Uuid,
    val state: State,
    val runAttemptCount: Int
)