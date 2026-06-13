package eu.tintera.background.guard

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.flow
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

internal actual fun switchableStateTokenProducer(
    context: PlatformContext,
    scope: CoroutineScope
): TokenProducer = SwitchableStateTokenProducer(
    state = observeAppBackgroundState(scope = scope),
    producers = mapOf(
        SwitchableState.BACKGROUND to WakeLockTokenProducer(
            context = context.context,
            scope = scope,
            timeout = context.wakelockTimeout
        ),
        SwitchableState.FOREGROUND to TokenProducer {
            flow {
                emit(
                    object : AbstractToken() {
                        override val tag = "ForegroundToken"
                        override suspend fun onRelease() {}
                        override fun onCancel() {}
                    }
                )
            }
        }
    )
)