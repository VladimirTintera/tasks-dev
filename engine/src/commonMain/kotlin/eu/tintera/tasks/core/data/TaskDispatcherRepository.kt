package eu.tintera.tasks.core.data

import eu.tintera.tasks.State
import kotlinx.coroutines.flow.Flow

interface TaskDispatcherRepository {
    fun dispatchableTasks(states: List<State>): Flow<List<DispatchableTask>>
}