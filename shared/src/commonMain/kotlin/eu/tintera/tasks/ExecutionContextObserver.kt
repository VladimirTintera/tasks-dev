package eu.tintera.tasks

import co.touchlab.kermit.Logger
import eu.tintera.guard.ExecutionContextObserver
import eu.tintera.guard.ExecutionContextObserverRegistry

class ExecutionContextObserver(
    registry: ExecutionContextObserverRegistry
) : ExecutionContextObserver {
    private val logger = Logger.withTag("ExecutionContextObserver")

    init {
        registry.registerObserver(this)
    }

    override fun onPreCancel() {
        logger.i { "onPreCancel called" }
    }

    override suspend fun onPreRelease() {
        logger.i { "onPreRelease called" }
    }

    override fun onStarted() {
        logger.i { "onStarted called" }
    }
}