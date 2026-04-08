package eu.tintera.tasks.core.locks

interface ExecutionContextObserverRegistry {
    fun registerObserver(observer: ExecutionContextObserver)
}