package eu.tintera.tasks.core.fakes

import eu.tintera.tasks.core.NetworkState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeNetworkState: NetworkState {
    val networkState = MutableStateFlow(NetworkState.State.Connected)
    override fun state(): Flow<NetworkState.State> = networkState
}