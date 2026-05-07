package eu.tintera.tasks.core.guard

import eu.tintera.guard.ExecutionContextObserver
import eu.tintera.guard.ExecutionContextObserverRegistry
import eu.tintera.guard.TokenProducer
import eu.tintera.guard.TokenProducerRegistry

class ExecutionContextBootstrapper(
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