package eu.tintera.background.tasks.core

import eu.tintera.background.tasks.State
import kotlin.uuid.Uuid

interface OrphanTaskSweeperRepository {
    suspend fun resetState(
        from: State,
        to: State,
        excludedIds: Set<Uuid>
    )
}