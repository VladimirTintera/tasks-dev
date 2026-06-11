package eu.tintera.guard

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.flowOf

actual class PlatformContext

internal actual fun switchableStateTokenProducer(
    context: PlatformContext,
    scope: CoroutineScope
): TokenProducer = TokenProducer {
    flowOf(object : AbstractToken() {
        override suspend fun onRelease() {}
        override fun onCancel() {}
        override val tag = "WebToken"
    })
}

