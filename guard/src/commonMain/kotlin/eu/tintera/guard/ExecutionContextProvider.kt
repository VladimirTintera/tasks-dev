package eu.tintera.guard

/**
 * A provider responsible for supplying an [ExecutionContext].
 * This is typically used to manage access to shared resources or execution environments.
 */
interface ExecutionContextProvider {
    /**
     * Acquires an [ExecutionContext].
     * This is a suspending function that may wait until a context becomes available.
     *
     * @return The acquired [ExecutionContext].
     */
    suspend fun acquire(): ExecutionContext
}

/**
 * Executes the given [block] within an acquired [ExecutionContext].
 * The context is automatically released after the block completes.
 *
 * @param block The action to perform within the context.
 */
suspend inline operator fun ExecutionContextProvider.invoke(
    crossinline block: suspend ExecutionContext.() -> Unit
) {
    acquire().use(block)
}