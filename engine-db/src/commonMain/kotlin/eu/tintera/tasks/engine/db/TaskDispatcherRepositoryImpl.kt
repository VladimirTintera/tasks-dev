package eu.tintera.tasks.engine.db

import eu.tintera.tasks.State
import eu.tintera.tasks.core.TaskDispatcherRepository
import eu.tintera.tasks.core.data.DispatchableTask
import eu.tintera.tasks.db.dao.DispatchableTaskDao
import eu.tintera.tasks.db.toEntityState
import eu.tintera.tasks.db.toTaskState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class TaskDispatcherRepositoryImpl(
    private val dispatchableTaskDao: DispatchableTaskDao
) : TaskDispatcherRepository {

    override fun dispatchableTasks(states: List<State>): Flow<List<DispatchableTask>> =
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