package eu.tintera.tasks.core.fakes

import eu.tintera.tasks.core.AppStateObserver
import kotlinx.coroutines.flow.StateFlow

class FakeAppStateObserver(override val isBackground: StateFlow<Boolean>) : AppStateObserver {
}