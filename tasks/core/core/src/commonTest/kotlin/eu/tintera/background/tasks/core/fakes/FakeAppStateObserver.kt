package eu.tintera.background.tasks.core.fakes

import eu.tintera.background.tasks.core.AppStateObserver
import kotlinx.coroutines.flow.StateFlow

class FakeAppStateObserver(override val isBackground: StateFlow<Boolean>) : AppStateObserver {
}