package eu.tintera.tasks.core.constraints

import eu.tintera.tasks.core.NetworkState
import eu.tintera.tasks.core.ProcessableTask
import kotlinx.coroutines.flow.map

internal class NetworkStateConstraint(
    private val networkState: NetworkState
) : Constraint {
    override fun hasConstraint(task: ProcessableTask) = task.networkRequired

    override fun isValid(task: ProcessableTask) = networkState.state().map {
        when (it) {
            NetworkState.State.Disconnected -> PreconditionResult.Unmet
            NetworkState.State.Connected -> PreconditionResult.Met
        }
    }

    override val monitorDuringExecution = true
}