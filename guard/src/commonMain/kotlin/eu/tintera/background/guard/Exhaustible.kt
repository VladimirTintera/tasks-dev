package eu.tintera.background.guard

import kotlinx.coroutines.flow.StateFlow


interface Exhaustible {
    val name: String // so diagnostics can say which producer ran out (e.g. "JVM Shutdown")
    val isExhausted: StateFlow<Boolean>
}

interface ExhaustibleObservable {
    val exhaustible: StateFlow<Set<Exhaustible>>
}


