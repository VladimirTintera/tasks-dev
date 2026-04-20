package eu.tintera.tasks

import eu.tintera.tasks.core.NetworkState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

internal class JvmNetworkState : NetworkState {
    override fun state(): Flow<NetworkState.State> = flowOf(NetworkState.State.Connected)
}