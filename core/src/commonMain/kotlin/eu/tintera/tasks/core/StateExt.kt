package eu.tintera.tasks.core

import eu.tintera.tasks.State

 val terminalStates: Set<State>
    get() = setOf(
        State.Cancelled, State.Succeeded, State.Failed
    )

val failedStates: Set<State>
    get() = setOf(State.Failed, State.Cancelled)

val runningStates = setOf(State.Enqueued, State.Blocked, State.Running)

val nonTerminalStates = State.entries.filterNot { it in terminalStates }.toSet()

fun State.terminal(): Boolean = when {
    terminalStates.contains(this) -> true
    else -> false
}

/**
 * Terminální stav můžeme nastavovat na cokoli
 * Neterminální pouze na neterminální stavy (nelze přeskočit z terminálního stavu na neterminální)
 */
fun State.allowedSourceStatesForChangeTo() = when {
    terminal() -> State.entries
    else -> nonTerminalStates
}