package eu.tintera.tasks

import co.touchlab.kermit.Logger
import eu.tintera.guard.Exhaustible
import eu.tintera.guard.ExhaustibleObservable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ExhaustibleObserver(
    private val observable: ExhaustibleObservable,
    scope: ApplicationScope,
    dispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    private val logger = Logger.withTag("ExhaustibleObserver")

    data class ExhaustibleInfo(
        val name: String,
        val isExhausted: Boolean,
    )

    private val source = flow {
        val seen = mutableSetOf<Exhaustible>()
        observable.exhaustible.collect { set ->
            set.forEach { exhaustible ->
                if (seen.add(exhaustible)) emit(exhaustible)
            }
        }
    }.map { exhaustible ->
        exhaustible.isExhausted.map {
            ExhaustibleInfo(
                name = exhaustible.name,
                isExhausted = it
            )
        }
    }.flattenMerge(concurrency = Int.MAX_VALUE)

    init {
        scope.launch(dispatcher) {
            source.collect { exhaustible ->
                logger.i { "${exhaustible.name}: exhausted = ${exhaustible.isExhausted}" }
            }
        }
    }
}