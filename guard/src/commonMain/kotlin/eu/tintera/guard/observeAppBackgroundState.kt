package eu.tintera.guard

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

internal expect fun  observeAppBackgroundState(scope: CoroutineScope) : StateFlow<Boolean>