package eu.tintera.guard

interface ExecutionContextObserverRegistry {
    fun registerObserver(observer: ExecutionContextObserver)
}