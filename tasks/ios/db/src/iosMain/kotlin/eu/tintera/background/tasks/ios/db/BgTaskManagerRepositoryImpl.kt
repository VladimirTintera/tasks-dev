package eu.tintera.background.tasks.ios.db

import eu.tintera.background.tasks.State
import eu.tintera.background.tasks.db.dao.SchedulableTaskDao
import eu.tintera.background.tasks.db.toEntityState
import eu.tintera.background.tasks.ios.BgTaskManagerRepository
import eu.tintera.background.tasks.ios.BgTaskManagerTask
import kotlin.time.Instant

internal class BgTaskManagerRepositoryImpl(
    private val dao: SchedulableTaskDao
) : BgTaskManagerRepository {
    override suspend fun tasks(
        states: List<State>
    ) = dao.schedulableTasks(
        states = states.map { it.toEntityState() }
    ).map {
        BgTaskManagerTask(
            id = it.id,
            processTime = it.processTime ?: Instant.DISTANT_PAST,
            requiresDeviceIdle = it.requiresDeviceIdle,
            networkRequired = it.networkRequired,
        )
    }
}