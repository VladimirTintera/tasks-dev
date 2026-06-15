package eu.tintera.background.tasks.core

import eu.tintera.background.guard.ExecutionEnvironmentConfig
import eu.tintera.background.guard.ExecutionEnvironmentFactory
import eu.tintera.background.guard.ExecutionContextProvider
import eu.tintera.background.guard.TokenProducer
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

fun TestScope.dispatchers() = object : AppDispatchers {
    override val default = StandardTestDispatcher(testScheduler)
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