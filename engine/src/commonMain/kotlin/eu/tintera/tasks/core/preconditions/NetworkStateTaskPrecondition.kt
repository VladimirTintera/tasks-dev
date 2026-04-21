package eu.tintera.tasks.core.preconditions

import eu.tintera.tasks.EventBus
import eu.tintera.tasks.core.NetworkState
import eu.tintera.tasks.core.data.ProcessableTask
import eu.tintera.tasks.core.data.Task
import kotlinx.coroutines.flow.map

internal class NetworkStateTaskPrecondition(
    private val networkState: NetworkState
) : TaskPrecondition {
    override fun hasConstraint(task: ProcessableTask) = task.networkRequired

    override fun isValid(task: ProcessableTask) = networkState.state().map {
        EventBus.send("NetworkStateTaskPrecondition", "state = $it")
        when (it) {
            NetworkState.State.Disconnected -> PreconditionResult.Unmet
            NetworkState.State.Connected -> PreconditionResult.Met
        }
    }

    override val monitorDuringExecution = true
}