package eu.tintera.tasks.engine.db

import eu.tintera.tasks.State
import eu.tintera.tasks.core.OrphanTaskSweeperRepository
import eu.tintera.tasks.db.dao.OrphanTaskDao
import eu.tintera.tasks.db.toEntityState
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