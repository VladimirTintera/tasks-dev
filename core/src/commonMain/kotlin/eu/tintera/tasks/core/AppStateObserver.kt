package eu.tintera.tasks.core

import kotlinx.coroutines.flow.StateFlow

interface AppStateObserver {
    val isBackground: StateFlow<Boolean>
}