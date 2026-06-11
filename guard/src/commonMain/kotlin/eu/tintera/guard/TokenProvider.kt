package eu.tintera.guard

/**
 * Interface for providing synchronization tokens.
 */
interface TokenProvider {

    suspend fun acquire(
        onPreCancel: () -> Unit = {},
        onCancel: () -> Unit
    ): Token
}