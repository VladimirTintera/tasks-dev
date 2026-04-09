package eu.tintera.guard

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

actual class PlatformContext

internal actual fun appStateAwareToken(
    context: PlatformContext,
    scope: CoroutineScope,
    isBackground: StateFlow<Boolean>,
    expirationHandler: () -> Unit
): Token = object : Token {
    override suspend fun release() {

    }

    override fun cancel() {

    }
}