package eu.tintera.background.guard

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow

actual class PlatformContext


internal actual fun switchableStateTokenProducer(
    context: PlatformContext,
    scope: CoroutineScope
): TokenProducer {
    val backgroundProducer = UiBackgroundTaskTokenProducer()

    return IosSwitchableStateTokenProducer(
        state = observeAppBackgroundState(scope = scope),
        foregroundProducer = TokenProducer {
            flow {
                emit(
                    object : AbstractToken() {
                        override val tag = "ForegroundToken"
                        override suspend fun onRelease() {}
                        override fun onCancel() {}
                    }
                )
            }
        },
        backgroundProducer = backgroundProducer,
        exhaustible = backgroundProducer
    )
}

private class IosSwitchableStateTokenProducer(
    private val state: StateFlow<SwitchableState>,
    private val foregroundProducer: TokenProducer,
    private val backgroundProducer: TokenProducer,
    private val exhaustible: Exhaustible
) : TokenProducer by SwitchableStateTokenProducer(
    state = state,
    producers = mapOf(
        SwitchableState.BACKGROUND to backgroundProducer,
        SwitchableState.FOREGROUND to foregroundProducer,
    )
), Exhaustible by exhaustible