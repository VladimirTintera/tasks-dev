package eu.tintera.tasks

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlin.time.Instant

object EventBus {
    val events: SharedFlow<TaskEvent>
        field = MutableSharedFlow(
            replay = 64,
            extraBufferCapacity = 64,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )

    fun send(event: TaskEvent) {
        events.tryEmit(event)
    }

    fun send(tag: String, message: String) = send(TaskEvent.Custom(tag, message))
}

sealed interface TaskEvent {
    data class TaskStarted(
        val identifier: String,
        val data: Data
    ) : TaskEvent

    data class TaskFinished(
        val identifier: String,
        val result: TaskResult
    ) : TaskEvent

    data class TaskFailed(
        val identifier: String,
        val message: String,
        val cause: Throwable?
    ) : TaskEvent

    data class BackgroundProcessingStarted(val time: Instant) : TaskEvent

    data class BackgroundProcessingExpirationCalled(val time: Instant) : TaskEvent

    data class BackgroundProcessingScheduling(val time: Instant) : TaskEvent

    data class BackgroundAlreadyPlaned(val forTime: Instant) : TaskEvent

    data class BackgroundProcessingCompleted(
        val wasSuccess: Boolean,
        val nextProcessTime: Instant?
    ) : TaskEvent

    data class BackgroundInitializationFailed(
        val code: Long,
        val description: String
    ) : TaskEvent

    data class Custom(
        val tag: String,
        val message: String
    ) : TaskEvent
}