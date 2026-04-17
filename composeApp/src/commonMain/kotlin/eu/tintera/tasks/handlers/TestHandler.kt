package eu.tintera.tasks.handlers

import eu.tintera.tasks.*
import eu.tintera.tasks.MainViewModel.Companion.DEFAULT_TAG
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

@Serializable
data class TestHandlerProgress(
    val totalCount: Int,
    val progress: Int
)

class TestHandler : TaskHandler<Int, Unit, TestHandlerProgress> {

    override suspend fun TaskScope<Int, TestHandlerProgress>.run(): TaskResult<Unit> {
        return merge(
            _interruptionEventBus.filter { it == taskId }.map {
                TaskResult.retry()
            },
            normalRun()
        ).first()
    }

    private fun TaskScope<Int, TestHandlerProgress>.normalRun() = flow {
        repeat(data) {
            setProgress(
                TestHandlerProgress(
                    totalCount = data,
                    progress = it
                )
            )
            delay(1.seconds)
        }
        emit(TaskResult.success(Unit))
    }

    companion object {
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
    taskRequest<TestHandler, Int>(
        data = count,
        tags = setOf(DEFAULT_TAG),
        constraints = Constraints(
            requiresDeviceIdle = false,
            requiresNetwork = true
        ),
    )
)