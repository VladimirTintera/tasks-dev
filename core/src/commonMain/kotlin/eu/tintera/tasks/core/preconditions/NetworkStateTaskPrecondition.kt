package eu.tintera.tasks.core.preconditions

import eu.tintera.tasks.EventBus
import eu.tintera.tasks.core.NetworkState
import eu.tintera.tasks.core.TaskPrecondition
import eu.tintera.tasks.core.data.Task
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class NetworkStateTaskPrecondition(
    private val networkState: NetworkState
) : TaskPrecondition {
    override fun hasConstraint(task: Task) = task.networkRequired

    override fun isValid(task: Task): Flow<Boolean> = networkState.state().map {
        EventBus.send("NetworkStateTaskPrecondition", "state = $it")
        it == NetworkState.State.Connected
    }

    override val monitorDuringExecution = true
}