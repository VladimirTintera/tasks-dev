package eu.tintera.background.guard

data class MultiplexerState(
    val isSystemTokenHeld: Boolean = false, // is a system token currently held?
    val activeTasksCount: Int = 0,          // how many contexts are actually working right now
    val isDebouncing: Boolean = false,      // are we inside the release-debounce window?
    /**
     * How many callers are suspended inside `acquire()` waiting for a token to be handed out.
     *
     * Without this, work that hangs because no producer yields a token is indistinguishable from
     * nothing happening at all: the caller is not counted in [activeTasksCount] until its token
     * arrives, so the whole state reads as idle. That is precisely the situation a diagnostic
     * observer is there to catch — "why is it not running" has to be answerable.
     */
    val awaitingCount: Int = 0
)