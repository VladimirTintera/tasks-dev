package eu.tintera.tasks

import androidx.sqlite.SQLiteDriver
import eu.tintera.guard.ExecutionEnvironment
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class IosTasksManagerConfiguration(
    val sqLiteDriver: SQLiteDriver? = null,
    val databaseName: String = "",
    val maxConcurrentTasks: Int = 10,
    val executionContextReleaseDebounce: Duration = 1.5.seconds,
    val bgProcessingTaskIdentifier: String? = null,
    val appRefreshTaskIdentifier: String? = null,
    val executionEnvironment: ExecutionEnvironment? = null,
) {
    init {
        bgProcessingTaskIdentifier?.also {
            require(!it.isBlank()) { "BG processing task identifier must be set" }
        }

        appRefreshTaskIdentifier?.also {
            require(!it.isBlank()) { "App refresh task identifier must be set" }
        }
    }
}