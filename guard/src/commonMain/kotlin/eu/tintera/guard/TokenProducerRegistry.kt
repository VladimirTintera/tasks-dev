package eu.tintera.guard

interface TokenProducerRegistry {
    fun registerProducer(producer: TokenProducer)
}