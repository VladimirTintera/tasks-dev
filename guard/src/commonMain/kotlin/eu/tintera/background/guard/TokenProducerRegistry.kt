package eu.tintera.background.guard

interface TokenProducerRegistry {
    fun registerProducer(producer: TokenProducer)
}