package eu.tintera.tasks.handlers

import co.touchlab.kermit.Logger
import eu.tintera.tasks.TaskHandler
import eu.tintera.tasks.TaskResult
import eu.tintera.tasks.TaskScope
import kotlinx.coroutines.delay
import org.koin.ext.getFullName
import kotlin.time.Duration.Companion.seconds

class TestHandler : TaskHandler {

    private val logger = Logger.withTag("TaskHandler")

    override suspend fun TaskScope.run(): TaskResult {

        logger.i { "Running test: ${this@TestHandler::class.getFullName()}, retryCount: $retryCount, data: $data" }
        delay(5.seconds)
        logger.i { "Test finished: ${this@TestHandler::class.getFullName()}, retryCount: $retryCount, data: $data" }
        return if (retryCount >= 2) TaskResult.success() else TaskResult.retry()
    }
}