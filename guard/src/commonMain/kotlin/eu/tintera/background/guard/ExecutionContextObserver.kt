package eu.tintera.background.guard

interface ExecutionContextObserver {
    fun onStarted() {}
    suspend fun onPreRelease() {}
    fun onPreCancel() {}
}