package eu.tintera.background.tasks.engine.db

import eu.tintera.background.tasks.State
import eu.tintera.background.tasks.core.TaskDispatcherRepository
import eu.tintera.background.tasks.core.data.DispatchableTask
import eu.tintera.background.tasks.db.dao.DispatchableTaskDao
import eu.tintera.background.tasks.db.toEntityState
import eu.tintera.background.tasks.db.toTaskState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class TaskDispatcherRepositoryImpl(
    private val dispatchableTaskDao: DispatchableTaskDao
) : TaskDispatcherRepository {

    override fun dispatchableTasks(states: Set<State>): Flow<List<DispatchableTask>> =
        dispatchableTaskDao.getDispatchableTasksByStates(
            states = states.map { it.toEntityState() }
        ).distinctUntilChanged().map {
            it.map { task ->
                DispatchableTask(
                    id = task.id,
                    state = task.state.toTaskState()
                )
            }
        }
}