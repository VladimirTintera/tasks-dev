package eu.tintera.background.tasks.core.constraints

import eu.tintera.background.tasks.core.NetworkState
import eu.tintera.background.tasks.core.ProcessableTask
import kotlinx.coroutines.flow.map

internal class NetworkStateConstraint(
    private val networkState: NetworkState
) : Constraint {
    override fun hasConstraint(task: ProcessableTask) = task.networkRequired

    override fun isValid(task: ProcessableTask) = networkState.state().map {
        when (it) {
            NetworkState.State.Disconnected -> ConstraintResult.Unmet
            NetworkState.State.Connected -> ConstraintResult.Met
        }
    }

    override val monitorDuringExecution = true
}