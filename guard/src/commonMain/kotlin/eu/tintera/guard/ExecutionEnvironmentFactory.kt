package eu.tintera.guard

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Duration.Companion.seconds

expect class PlatformContext

object ExecutionEnvironmentFactory {

    fun createDefault(
        context: PlatformContext,
        scope: CoroutineScope,
        config: ExecutionContextConfig = ExecutionContextConfig(releaseDebounce = 1.5.seconds),
        tokenProducers: List<TokenProducer> = emptyList(),
        observers: List<ExecutionContextObserver> = emptyList()
    ): ExecutionEnvironment {

        val tokenProvider = CompositeTokenProvider(
            scope = scope,
            producers = listOf(
                AppStateAwareTokenProducer { expirationHandler ->
                    appStateAwareToken(
                        context = context,
                        scope = scope,
                        isBackground = observeAppBackgroundState(scope),
                        expirationHandler = expirationHandler
                    )
                }
            ) + tokenProducers
        )

        val lifecycleRegistry = CompositeExecutionContextObserver(observers)

        val contextProvider = SharedExecutionContextProvider(
            tokenProvider = tokenProvider,
            scope = scope,
            config = config,
            lifecycleObserver = lifecycleRegistry
        )

        return object : ExecutionEnvironment, ExecutionContextProvider by contextProvider, TokenRegistry by tokenProvider, ExecutionContextObserverRegistry by lifecycleRegistry {

        }
    }
}

internal expect fun appStateAwareToken(
    context: PlatformContext,
    scope: CoroutineScope,
    isBackground: StateFlow<Boolean>,
    expirationHandler: () -> Unit
): Token