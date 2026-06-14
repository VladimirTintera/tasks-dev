package eu.tintera.background.tasks.core.cleanup

fun interface DatabaseCleanupService {
    suspend fun cleanup()
}