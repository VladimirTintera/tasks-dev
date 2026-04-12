package eu.tintera.tasks.handlers

import eu.tintera.tasks.TaskHandler
import eu.tintera.tasks.TaskResult
import eu.tintera.tasks.TaskScope
import eu.tintera.tasks.taskDataOf
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

class TestHandler : TaskHandler {

    override suspend fun TaskScope.run(): TaskResult {
        return merge(
            _interruptionEventBus.filter { it == taskId }.map {
                TaskResult.retry()
            },
            normalRun()
        ).first()
    }

    private fun TaskScope.normalRun() = flow {
        repeat(20) {
            setProgress(
                taskDataOf("total" to 20, "current" to it)
            )
            delay(1.seconds)
        }
        emit(TaskResult.success())
    }

    companion object {
        private val _interruptionEventBus = MutableSharedFlow<Uuid>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
        fun interrupt(id: Uuid) {
            _interruptionEventBus.tryEmit(id)
        }
    }
}