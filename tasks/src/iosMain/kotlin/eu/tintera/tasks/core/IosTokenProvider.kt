package eu.tintera.tasks.core

import eu.tintera.guard.CompositeTokenProducerProvider
import eu.tintera.guard.TokenProducer
import eu.tintera.guard.TokenProvider

internal class IosTokenProvider(
    private val scope: ApplicationScope,
    private val dispatchers: AppDispatchers,
    private val tokenProducers: List<TokenProducer>
) : TokenProvider by CompositeTokenProducerProvider(
    scope = scope,
    dispatcher = dispatchers.default,
    producers = tokenProducers
)