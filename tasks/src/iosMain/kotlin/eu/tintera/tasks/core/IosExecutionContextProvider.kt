package eu.tintera.tasks.core

import eu.tintera.guard.CompositeExecutionContextObserver
import eu.tintera.guard.ExecutionContextConfig
import eu.tintera.guard.ExecutionContextProvider
import eu.tintera.guard.SharedExecutionContextProvider

internal class IosExecutionContextProvider(
    tokenProvider: IosTokenProvider,
    scope: ApplicationScope,
    dispatchers: AppDispatchers,
    config: ExecutionContextConfig,
    lifecycleObserver: CompositeExecutionContextObserver
) : ExecutionContextProvider by SharedExecutionContextProvider(
    tokenProvider = tokenProvider,
    scope = scope,
    config = config,
    dispatcher = dispatchers.default,
    lifecycleObserver = lifecycleObserver
)