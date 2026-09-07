package eu.tintera.background.guard

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import platform.Foundation.NSError
import platform.HealthKit.HKHealthStore
import platform.HealthKit.HKObserverQuery
import platform.HealthKit.HKSampleType
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid


/**
 * A [TokenProducer] implementation that wraps an [HKObserverQuery].
 *
 * This class listens for background updates from HealthKit and provides a [Token]
 * for each invocation. It ensures that the HealthKit completion handler is called
 * when the token is released or after a [timeout], allowing the OS to manage
 * background execution time.
 *
 * @param store The [HKHealthStore] instance to execute the query on.
 * @param type The [HKSampleType] to observe for changes.
 * @param timeout The maximum duration to wait for processing before automatically releasing the token.
 */
abstract class HkObserverQueryTokenProvider(
    scope: CoroutineScope,
    private val store: HKHealthStore,
    private val type: HKSampleType,
    private val timeout: Duration = 10.seconds
) : PendingTokenProducer(scope) {
    abstract fun onError(error: NSError)

    /**
     * Starts the observer query. Called **after** construction, never from `init`.
     *
     * The query callback reaches abstract [onError], so starting it from the constructor would
     * publish a `this` whose subclass is not built yet: a subclass assigns its own fields only
     * after the super constructor returns, and HealthKit may invoke the handler straight away.
     * The same leak crashed `PluginSyncEnabler` in the SDK with an NPE on a field that looked
     * non-null. Publishing `this` from a constructor also voids the memory model guarantee for
     * final fields, so it is not merely a start-up race.
     *
     * Register the producer with the execution environment first, then call this — a token
     * produced before registration is not lost (the pending set is a `StateFlow`), but there is
     * no reason to hand one out before anyone can take it.
     */
    fun start() {
        val observer = HKObserverQuery(
            sampleType = type,
            predicate = null,
        ) { _, completionHandler, error ->
            if (error != null) {
                onError(error)
            }

            completionHandler?.also { handler ->

                val token = HkToken(
                    type = type,
                    completionHandler = handler,
                    scope = scope,
                    timeout = timeout
                )

                produce(token)
            }
        }

        store.executeQuery(observer)
    }
}

class HkToken(
    val type: HKSampleType,
    scope: CoroutineScope,
    timeout: Duration,
    private val completionHandler: () -> Unit
) : AbstractToken() {

    override val tag = "HkObserverQueryToken:${type.identifier}:${Uuid.random().toString().take(4)}"

    private val cancelJob = scope.launch {
        delay(timeout)
        finishWithCancel()
    }

    override suspend fun onRelease() {
        cancelJob.cancel()
        completionHandler()
    }

    override fun onCancel() {
        cancelJob.cancel()
        completionHandler()
    }
}