package eu.tintera.background.tasks.core.fakes

import eu.tintera.background.tasks.core.NetworkState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeNetworkState(
    initial: NetworkState.State = NetworkState.State.Connected
): NetworkState {
    val networkState = MutableStateFlow(initial)
    override fun state(): Flow<NetworkState.State> = networkState
}