package eu.tintera.background.tasks.runtime

import eu.tintera.background.tasks.core.AppStateObserver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WebAppStateObserver : AppStateObserver {
    private val state = MutableStateFlow(false)
    override val isBackground: StateFlow<Boolean> = state.asStateFlow()
}