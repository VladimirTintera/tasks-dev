package eu.tintera.tasks.core

import eu.tintera.tasks.EventBus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

internal class ExecutionCapabilityEvaluator(
    private val providers: List<ExecutionCapabilityProvider>,
    private val appStateObserver: AppStateObserver
) {
    init {
        EventBus.send("ExecutionCapabilityProvider", "provider = $providers")
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    fun capabilities(): Flow<Set<ExecutionCapability>> = appStateObserver.isBackground.flatMapLatest { isBg ->
        when {
            !isBg -> flowOf(setOf(ExecutionCapability.SHORT_LIVED, ExecutionCapability.HEAVY_PROCESSING))
            providers.isEmpty() -> flowOf(setOf())
            else -> combine(providers.map { it.capabilities() }) { all ->
                all.flatMap { it }.toSet()
            }
        }
    }
}