package eu.tintera.guard

import kotlinx.coroutines.flow.*


internal class ObservableRegistry : ExhaustibleObservable, PendingTokenObservable {

    private val _exhaustible = MutableStateFlow<Set<Exhaustible>>(emptySet())
    override val exhaustible: StateFlow<Set<Exhaustible>> = _exhaustible.asStateFlow()

    private val _pendingToken = MutableStateFlow<Set<PendingTokenObservable>>(emptySet())

    override val pendingToken = _pendingToken.flatMapLatest { observables ->
        if (observables.isEmpty()) flowOf(emptySet())
        else combine(observables.map { it.pendingToken }) { array ->
            array.flatMap { it }.toSet()
        }
    }

    fun tryRegister(component: Any) {

        if (component is Exhaustible) {
            _exhaustible.update { it + component }
        }

        if (component is PendingTokenObservable) {
            _pendingToken.update { it + component }
        }
    }
}