package eu.tintera.background.guard

interface ExecutionContextObserverRegistry {
    fun registerObserver(observer: ExecutionContextObserver)
}