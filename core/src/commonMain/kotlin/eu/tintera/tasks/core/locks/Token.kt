package eu.tintera.tasks.core.locks

/**
 * Represents a handle to a held lock or a pending acquisition request.
 */
interface Token {
    /**
     * Releases the lock associated with this token.
     * This is a suspending operation that ensures the lock state is updated correctly.
     */
    suspend fun release()

    /**
     * Cancels the pending request for the lock.
     */
    fun cancel()
}