package eu.tintera.tasks.core

import kotlinx.coroutines.flow.Flow

interface NetworkState {

    enum class State {
        Connected,
        Disconnected
    }

    fun state(): Flow<State>
}