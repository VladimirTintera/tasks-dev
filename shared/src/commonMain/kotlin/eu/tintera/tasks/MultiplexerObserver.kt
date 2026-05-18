package eu.tintera.tasks

import co.touchlab.kermit.Logger
import eu.tintera.guard.MultiplexerObservable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MultiplexerObserver(
    scope: ApplicationScope,
    private val observable: MultiplexerObservable,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {

    private val logger = Logger.withTag("MultiplexerObserver")

    init {
        scope.launch(dispatcher) {
            observable.state.collect { state ->
                logger.i { "MultiplexerState: '${state}'" }
            }
        }
    }
}