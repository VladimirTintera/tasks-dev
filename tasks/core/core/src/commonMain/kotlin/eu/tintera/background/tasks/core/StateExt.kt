package eu.tintera.background.tasks.core

import eu.tintera.background.tasks.State

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
 * A terminal state may be set from anything.
 * A non-terminal state only from another non-terminal one — there is no way back out of a terminal state.
 */
fun State.allowedSourceStatesForChangeTo() = when {
    terminal() -> State.entries
    else -> nonTerminalStates
}