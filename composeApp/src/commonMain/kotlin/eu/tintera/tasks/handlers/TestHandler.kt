package eu.tintera.tasks.handlers

import eu.tintera.tasks.Data
import eu.tintera.tasks.LegacyTaskHandler
import eu.tintera.tasks.TaskHandler
import eu.tintera.tasks.TaskResult
import eu.tintera.tasks.TaskScope
import eu.tintera.tasks.taskDataOf
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

class TestHandler : LegacyTaskHandler {

    override suspend fun TaskScope<Data, Data>.run(): TaskResult<Data> {
        return merge(
            _interruptionEventBus.filter { it == taskId }.map {
                TaskResult.retry()
            },
            normalRun()
        ).first()
    }

    private fun TaskScope<Data, Data>.normalRun() = flow {
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