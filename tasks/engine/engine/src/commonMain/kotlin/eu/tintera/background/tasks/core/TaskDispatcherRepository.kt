package eu.tintera.background.tasks.core

import eu.tintera.background.tasks.State
import eu.tintera.background.tasks.core.data.DispatchableTask
import kotlinx.coroutines.flow.Flow

interface TaskDispatcherRepository {
    fun dispatchableTasks(states: Set<State>): Flow<List<DispatchableTask>>
}