package eu.tintera.background.tasks

import co.touchlab.kermit.Logger
import eu.tintera.background.guard.ExecutionContextObserver
import eu.tintera.background.guard.ExecutionContextObserverRegistry

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