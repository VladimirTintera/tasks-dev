package eu.tintera.tasks.core.locks

/**
 * Interface for providing synchronization tokens.
 */
interface TokenProvider {
    /**
     * Acquires a new [Token].
     *
     * @param expirationHandler A callback invoked when the acquired token expires
     * or is invalidated by the provider.
     * @return The acquired [Token] instance.
     */
    suspend fun acquire(expirationHandler: () -> Unit): Token
}