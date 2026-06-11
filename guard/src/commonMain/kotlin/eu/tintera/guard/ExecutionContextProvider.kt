package eu.tintera.guard

interface ExecutionContextProvider {
    /**
     * Acquires an [ExecutionContext].
     * This is a suspending function that may wait until a context becomes available.
     *
     * @return The acquired [ExecutionContext].
     */
    suspend fun acquire(): ExecutionContext
}

suspend inline operator fun ExecutionContextProvider.invoke(
    crossinline block: suspend ExecutionContext.() -> Unit
) {
    acquire().use(block)
}