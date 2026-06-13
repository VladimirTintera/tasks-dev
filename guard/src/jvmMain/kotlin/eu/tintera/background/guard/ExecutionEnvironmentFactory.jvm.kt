package eu.tintera.background.guard

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow

actual class PlatformContext

internal actual fun switchableStateTokenProducer(
    context: PlatformContext,
    scope: CoroutineScope
): TokenProducer = JvmShutdownTokenProducer()

internal class JvmShutdownTokenProducer : ExhaustibleTokenProducer(
    name = "JvmShutdown"
) {
    private val runtime = Runtime.getRuntime()
    override suspend fun produce(): Token = ShutdownToken(runtime)
}

private class ShutdownToken(
    private val runtime: Runtime
) : AbstractToken() {
    override val tag = "JvmShutdownToken"

    private val hook = Thread {
        finishWithCancel()
        Thread.sleep(1000)
    }.also {
        runtime.addShutdownHook(it)
    }

    override suspend fun onRelease() {
        runtime.removeShutdownHook(hook)
    }

    override fun onCancel() {}
}
