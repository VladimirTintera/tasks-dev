package eu.tintera.background.tasks.engine.db

import eu.tintera.background.tasks.State
import eu.tintera.background.tasks.core.OrphanTaskSweeperRepository
import eu.tintera.background.tasks.db.dao.OrphanTaskDao
import eu.tintera.background.tasks.db.toEntityState
import kotlin.uuid.Uuid

internal class OrphanTaskRepositoryImpl(
    private val dao: OrphanTaskDao
) : OrphanTaskSweeperRepository {
    override suspend fun resetState(
        from: State,
        to: State,
        excludedIds: Set<Uuid>
    ) {
        if (excludedIds.isEmpty()) dao.resetState(
            from = from.toEntityState(),
            to = to.toEntityState()
        ) else dao.resetStateWithExclusion(
            from = from.toEntityState(),
            to = to.toEntityState(),
            excludedIds = excludedIds
        )
    }
}