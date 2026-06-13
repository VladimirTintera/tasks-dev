package eu.tintera.background.guard

import kotlinx.coroutines.flow.StateFlow

interface MultiplexerObservable {
    val state: StateFlow<MultiplexerState>
}