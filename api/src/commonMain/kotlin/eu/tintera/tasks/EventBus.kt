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

    data class Custom(
        val tag: String,
        val message: String
    ) : TaskEvent
}