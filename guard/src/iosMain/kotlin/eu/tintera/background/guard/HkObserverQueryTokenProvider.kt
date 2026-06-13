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
    store: HKHealthStore,
    type: HKSampleType,
    timeout: Duration = 10.seconds
) : PendingTokenProducer(scope) {
    abstract fun onError(error: NSError)

    init {
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