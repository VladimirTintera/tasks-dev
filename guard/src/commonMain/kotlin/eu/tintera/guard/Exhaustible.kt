package eu.tintera.guard

import kotlinx.coroutines.flow.StateFlow


interface Exhaustible {
    val name: String // Aby debug UI vědělo, kdo přesně je vyčerpán (např. "JVM Shutdown")
    val isExhausted: StateFlow<Boolean>
}

interface ExhaustibleObservable {
    val exhaustible: StateFlow<Set<Exhaustible>>
}


