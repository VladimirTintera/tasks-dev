package eu.tintera.guard

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

object EventBus {
    val events: SharedFlow<GuardEvent>
        field = MutableSharedFlow(
            replay = 64,
            extraBufferCapacity = 64,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )

    fun send(event: GuardEvent) {
        events.tryEmit(event)
    }

    fun send(tag: String, message: String) = send(GuardEvent(tag, message))
}

data class GuardEvent(
    val tag: String,
    val message: String
)