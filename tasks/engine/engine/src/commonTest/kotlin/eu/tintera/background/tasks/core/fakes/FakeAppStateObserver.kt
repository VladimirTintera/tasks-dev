package eu.tintera.background.tasks.core.fakes

import kotlinx.coroutines.flow.StateFlow

class FakeAppStateObserver(override val isBackground: StateFlow<Boolean>) : AppStateObserver {
}