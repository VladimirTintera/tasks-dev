package eu.tintera.tasks.core

import eu.tintera.tasks.State
import eu.tintera.tasks.core.data.DispatchableTask
import kotlinx.coroutines.flow.Flow

interface TaskDispatcherRepository {
    fun dispatchableTasks(states: List<State>): Flow<List<DispatchableTask>>
}