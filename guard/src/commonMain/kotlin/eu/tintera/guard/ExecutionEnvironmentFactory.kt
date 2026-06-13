package eu.tintera.guard

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.time.Duration.Companion.seconds

expect class PlatformContext

object ExecutionEnvironmentFactory {

    fun create(
        scope: CoroutineScope,
        tokenProducers: List<TokenProducer>,
        config: ExecutionEnvironmentConfig = ExecutionEnvironmentConfig(releaseDebounce = 1.5.seconds),
        observers: List<ExecutionContextObserver> = emptyList(),
        dispatcher: CoroutineDispatcher = Dispatchers.Default
    ): ExecutionEnvironment {

        require(tokenProducers.isNotEmpty()) { "There must be at least one tokenProducer" }

        val observableRegistry = ObservableRegistry()

        tokenProducers.forEach { observableRegistry.tryRegister(it) }

        val observerRegistry = CompositeExecutionContextObserver(
            observers + tokenProducers.flatMap { it.providedObservers }
        )

        val tokenProducer = CompositeTokenProducer(
            scope = scope,
            producers = tokenProducers,
            dispatcher = dispatcher
        ) { producer ->
            observableRegistry.tryRegister(producer)
            producer.providedObservers.forEach {
                observerRegistry.registerObserver(it)
            }
        }

        val contextProvider = SharedExecutionContextProvider(
            tokenProducer = tokenProducer,
            scope = scope,
            config = config,
            lifecycleObserver = observerRegistry,
            dispatcher = dispatcher
        )

        return object : ExecutionEnvironment,
            ExecutionContextProvider by contextProvider,
            TokenProducerRegistry by tokenProducer,
            TokenObservable by tokenProducer,
            ExhaustibleObservable by observableRegistry,
            PendingTokenObservable by observableRegistry,
            MultiplexerObservable by contextProvider,
            ExecutionContextObserverRegistry by observerRegistry {}
    }

    fun create(
        context: PlatformContext,
        scope: CoroutineScope,
        config: ExecutionEnvironmentConfig = ExecutionEnvironmentConfig(releaseDebounce = 1.5.seconds),
        observers: List<ExecutionContextObserver> = emptyList(),
        additionalTokenProviders: List<TokenProducer> = emptyList()
    ): ExecutionEnvironment = create(
        scope = scope,
        tokenProducers = listOf(defaultTokenProducer(context, scope)) + additionalTokenProviders,
        config = config,
        observers = observers
    )
}

fun defaultTokenProducer(
    context: PlatformContext,
    scope: CoroutineScope,
): TokenProducer = switchableStateTokenProducer(
    context = context,
    scope = scope,
)

internal expect fun switchableStateTokenProducer(
    context: PlatformContext,
    scope: CoroutineScope,
): TokenProducer