package eu.tintera.tasks.core

import eu.tintera.tasks.EventBus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

internal class ExecutionWindowEvaluator(
    private val providers: List<ExecutionWindowProvider>,
    private val appStateObserver: AppStateObserver
) {
    init {
        EventBus.send("ExecutionCapabilityProvider", "provider = $providers")
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    fun capabilities(): Flow<Set<ExecutionWindo>> = appStateObserver.isBackground.flatMapLatest { isBg ->
        when {
            !isBg -> flowOf(setOf(ExecutionWindo.SHORT))
            providers.isEmpty() -> flowOf(emptySet())
            else -> combine(providers.map { it.capabilities() }) { all ->
                all.flatMap { it }.toSet()
            }
        }
    }
}