package eu.tintera.background.tasks.core.guard

import eu.tintera.background.guard.ExecutionContextObserver
import eu.tintera.background.guard.ExecutionContextObserverRegistry
import eu.tintera.background.guard.TokenProducer
import eu.tintera.background.guard.TokenProducerRegistry

internal class ExecutionContextBootstrapper(
    private val observerRegistry: ExecutionContextObserverRegistry,
    private val tokenProducerRegistry: TokenProducerRegistry,
    observers: List<ExecutionContextObserver>,
    tokenProducers: List<TokenProducer>
) {
    init {
        observers.forEach { observerRegistry.registerObserver(it) }
        tokenProducers.forEach { tokenProducerRegistry.registerProducer(it) }
    }
}