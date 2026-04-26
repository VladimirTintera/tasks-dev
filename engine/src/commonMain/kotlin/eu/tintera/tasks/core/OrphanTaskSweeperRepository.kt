package eu.tintera.tasks.core

import eu.tintera.tasks.State
import kotlin.uuid.Uuid

interface OrphanTaskSweeperRepository {
    suspend fun resetState(
        from: State,
        to: State,
        excludedIds: Set<Uuid>
    )
}