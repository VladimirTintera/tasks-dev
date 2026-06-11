package eu.tintera.guard

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow

internal class SwitchableStateTokenProducer<T>(
    private val state: StateFlow<T>,
    private val producers: Map<T, TokenProducer>
) : TokenProducer {

    override val providedObservers = producers.values.flatMap { it.providedObservers }

    private val foregroundTokenProducer = TokenProducer {
        flow {
            emit(object : AbstractToken() {
                override val tag = "ForegroundToken"
                override suspend fun onRelease() {}
                override fun onCancel() {}
            })
        }
    }

    override fun token(): Flow<Token> = state.flatMapLatest {
        producers[it]?.token() ?: emptyFlow()
    }.autoReleasePrevious()

}