package eu.tintera.tasks.core

import eu.tintera.tasks.core.locks.ExecutionContextConfig
import eu.tintera.tasks.core.locks.ExecutionContextProvider
import eu.tintera.tasks.core.locks.SharedExecutionContextProvider

internal class IosExecutionContextProvider(
    tokenProvider: IosTokenProvider,
    scope: ApplicationScope,
    dispatchers: AppDispatchers,
    config: ExecutionContextConfig
) : ExecutionContextProvider by SharedExecutionContextProvider(
    tokenProvider = tokenProvider,
    scope = scope,
    config = config,
    dispatchers = dispatchers
)