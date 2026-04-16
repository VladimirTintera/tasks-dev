package eu.tintera.tasks.handlers

import eu.tintera.tasks.*
import eu.tintera.tasks.MainViewModel.Companion.DEFAULT_TAG
import eu.tintera.tasks.handlers.TestHandler.Companion.COUNT
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

data class TestHandlerProgress(
    val totalCount: Int,
    val progress: Int
)

class TestHandler : LegacyTaskHandler {

    override suspend fun LegacyTaskScope.run(): LegacyTaskResult {
        return merge(
            _interruptionEventBus.filter { it == taskId }.map {
                TaskResult.retry()
            },
            normalRun()
        ).first()
    }

    private fun LegacyTaskScope.normalRun() = flow {
        repeat(data.getInt(COUNT) ?: 0) {
            setProgress(
                taskDataOf("total" to 20, "current" to it)
            )
            delay(1.seconds)
        }
        emit(TaskResult.success())
    }

    companion object {
        internal const val COUNT = "count"
        private val _interruptionEventBus =
            MutableSharedFlow<Uuid>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

        fun interrupt(id: Uuid) {
            _interruptionEventBus.tryEmit(id)
        }


    }
}

suspend fun TaskManager.scheduleTestHandler(
    count: Int
) = enqueueTask(
    taskRequest<TestHandler>(
        tags = setOf(DEFAULT_TAG),
        constraints = Constraints(
            requiresDeviceIdle = false,
            requiresNetwork = true
        ),
        data = taskDataOf(COUNT to count)
    )
)