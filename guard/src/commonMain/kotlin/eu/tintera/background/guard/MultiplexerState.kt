package eu.tintera.background.guard

data class MultiplexerState(
    val isSystemTokenHeld: Boolean = false, // is a system token currently held?
    val activeTasksCount: Int = 0,          // how many contexts are actually working right now
    val isDebouncing: Boolean = false       // are we inside the release-debounce window?
)