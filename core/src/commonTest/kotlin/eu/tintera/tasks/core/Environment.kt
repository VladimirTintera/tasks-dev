package eu.tintera.tasks.core

import eu.tintera.guard.ExecutionEnvironmentConfig
import eu.tintera.guard.ExecutionEnvironmentFactory
import eu.tintera.guard.ExecutionContextProvider
import eu.tintera.guard.TokenProducer
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
    tokenProducer: TokenProducer,
    releaseDebounce: Duration = defaultReleaseDebounce
): ExecutionContextProvider = ExecutionEnvironmentFactory.create(
    scope = this,
    tokenProducers = listOf(tokenProducer),
    config = ExecutionEnvironmentConfig(releaseDebounce)
)