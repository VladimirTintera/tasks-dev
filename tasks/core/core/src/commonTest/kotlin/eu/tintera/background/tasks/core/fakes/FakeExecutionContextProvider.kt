package eu.tintera.background.tasks.core.fakes

import eu.tintera.background.guard.ExecutionContext
import eu.tintera.background.guard.ExecutionContextProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class FakeExecutionContextProvider : ExecutionContextProvider {
    private val expired = MutableStateFlow(false)

    val token = object : ExecutionContext {

        override suspend fun release() {

        }

        override val isExpired: StateFlow<Boolean>
            get() = expired
    }

    override suspend fun acquire(): ExecutionContext = token

    fun cancel() {
        expired.update { true }
    }

}