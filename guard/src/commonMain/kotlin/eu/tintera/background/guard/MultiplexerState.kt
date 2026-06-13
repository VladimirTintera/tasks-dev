package eu.tintera.background.guard

data class MultiplexerState(
    val isSystemTokenHeld: Boolean = false, // Držíme aktuálně systémový token?
    val activeTasksCount: Int = 0,          // Kolik kontextů zrovna reálně pracuje?
    val isDebouncing: Boolean = false       // Jsme v onom "release debounce" okně?
)