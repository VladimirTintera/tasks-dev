package eu.tintera.background.tasks

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlin.time.Instant

object EventBus {
    private val _events = MutableSharedFlow<TaskEvent>(
        replay = 64,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<TaskEvent> = _events.asSharedFlow()

    fun send(event: TaskEvent) {
        _events.tryEmit(event)
    }

    fun send(tag: String, message: String) = send(TaskEvent.Custom(tag, message))
}

sealed interface TaskEvent {

    data class Custom(
        val tag: String,
        val message: String
    ) : TaskEvent
}