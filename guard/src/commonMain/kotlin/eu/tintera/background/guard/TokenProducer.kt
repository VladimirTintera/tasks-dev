package eu.tintera.background.guard

import kotlinx.coroutines.flow.Flow

fun interface TokenProducer {
    fun token(): Flow<Token>

    val providedObservers: List<ExecutionContextObserver>
        get() = emptyList()
}