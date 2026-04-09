package eu.tintera.guard

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal actual fun observeAppBackgroundState(
    scope: CoroutineScope
): StateFlow<Boolean> = MutableStateFlow(false).asStateFlow()