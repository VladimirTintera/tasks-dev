package eu.tintera.tasks.core.cleanup

fun interface DatabaseCleanupService {
    suspend fun cleanup()
}