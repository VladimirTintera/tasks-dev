package eu.tintera.tasks

import eu.tintera.tasks.core.AppDispatchers
import eu.tintera.tasks.core.ApplicationScope
import eu.tintera.tasks.core.locks.ExecutionContextConfig
import eu.tintera.tasks.core.locks.ExecutionContextProvider
import eu.tintera.tasks.core.locks.SharedExecutionContextProvider

internal class JvmExecutionContextProvider(
    tokenProvider: JvmTokenProvider,
    scope: ApplicationScope,
    dispatchers: AppDispatchers,
    config: ExecutionContextConfig
) : ExecutionContextProvider by SharedExecutionContextProvider(
    tokenProvider = tokenProvider,
    scope = scope,
    config = config,
    dispatcher = dispatchers.default
)