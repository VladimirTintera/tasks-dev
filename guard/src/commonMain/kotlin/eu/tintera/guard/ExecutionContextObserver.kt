package eu.tintera.guard

interface ExecutionContextObserver {
    fun onStarted() {}
    suspend fun onPreRelease() {}
    fun onPreCancel() {}
}