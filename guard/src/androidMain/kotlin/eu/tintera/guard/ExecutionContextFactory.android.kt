package eu.tintera.guard

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

actual class PlatformContext(
    val context: Context,
    val wakelockTimeout: Duration = 30.seconds
) {
    init {
        require(wakelockTimeout.isPositive()) { "wakelockTimeout must be positive" }
    }
}

internal actual fun appStateAwareToken(
    context: PlatformContext,
    scope: CoroutineScope,
    isBackground: StateFlow<Boolean>,
    expirationHandler: () -> Unit
): Token = AndroidWakeLockToken(
    context = context.context,
    isBackground = isBackground,
    scope = scope,
    timeout = context.wakelockTimeout,
    expirationHandler = expirationHandler
)
