package eu.tintera.background.tasks.runtime

import eu.tintera.background.tasks.core.NetworkState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

internal class JvmNetworkState : NetworkState {
    override fun state(): Flow<NetworkState.State> = flowOf(NetworkState.State.Connected)
}