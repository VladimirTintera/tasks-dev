package eu.tintera.tasks

import eu.tintera.tasks.core.AppStateObserver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class JvmAppStateObserver : AppStateObserver {
    private val state = MutableStateFlow(false)
    override val isBackground: StateFlow<Boolean> = state.asStateFlow()
}