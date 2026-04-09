package eu.tintera.tasks.core

import eu.tintera.guard.CompositeTokenProvider
import eu.tintera.guard.TokenProducer
import eu.tintera.guard.TokenProvider

internal class IosTokenProvider(
    private val scope: ApplicationScope,
    private val dispatchers: AppDispatchers,
    private val tokenProducers: List<TokenProducer>
) : TokenProvider by CompositeTokenProvider(
    scope = scope,
    dispatcher = dispatchers.default,
    producers = tokenProducers
)