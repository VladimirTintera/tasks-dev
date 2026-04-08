package eu.tintera.tasks.core

import eu.tintera.tasks.core.locks.CompositeTokenProvider
import eu.tintera.tasks.core.locks.TokenProducer
import eu.tintera.tasks.core.locks.TokenProvider

internal class IosTokenProvider(
    private val scope: ApplicationScope,
    private val dispatchers: AppDispatchers,
    private val tokenProducers: List<TokenProducer>
) : TokenProvider by CompositeTokenProvider(
    scope = scope,
    dispatcher = dispatchers.default,
    producers = tokenProducers
)