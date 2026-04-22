package eu.tintera.tasks.core

import eu.tintera.guard.ExecutionEnvironmentConfig
import eu.tintera.guard.SharedExecutionContextProvider
import eu.tintera.guard.TokenProvider
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

fun TestScope.dispatchers() = object : AppDispatchers {
    override val default = StandardTestDispatcher(testScheduler)
    override val io = StandardTestDispatcher(testScheduler)
    override val main = StandardTestDispatcher(testScheduler)
}

val defaultReleaseDebounce = 1.5.seconds

fun TestScope.executionContextProvider(
    tokenProvider: TokenProvider,
    releaseDebounce: Duration = defaultReleaseDebounce
) = dispatchers().let {
    SharedExecutionContextProvider(
        tokenProvider = tokenProvider,
        scope = ApplicationScope(SupervisorJob()),
        dispatcher = it.default,
        config = ExecutionEnvironmentConfig(releaseDebounce)
    )
}