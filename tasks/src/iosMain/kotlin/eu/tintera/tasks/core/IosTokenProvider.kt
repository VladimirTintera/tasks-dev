package eu.tintera.tasks.core

import eu.tintera.tasks.core.locks.ReactiveCompositeTokenProvider
import eu.tintera.tasks.core.locks.TokenProvider

internal class IosTokenProvider(
    private val scope: ApplicationScope,
    private val dispatchers: AppDispatchers,
    private val bgTaskManager: BgTaskManager,
    private val appLifecycleManager: AppLifecycleManager
) : TokenProvider by ReactiveCompositeTokenProvider(
    scope = scope,
    dispatcher = dispatchers.default,
    producers = listOf(
        bgTaskManager,
        appLifecycleManager
    )
)