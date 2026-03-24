package eu.tintera.tasks.core

import eu.tintera.tasks.core.locks.ExecutionContext
import eu.tintera.tasks.core.locks.ExecutionContextProvider
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