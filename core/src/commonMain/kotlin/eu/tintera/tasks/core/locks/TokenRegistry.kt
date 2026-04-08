package eu.tintera.tasks.core.locks

/**
 * Rozhraní pro kohokoliv, kdo chce do systému přidat vlastní zdroj probuzení.
 */
interface TokenRegistry {
    fun registerProducer(producer: TokenProducer)
}