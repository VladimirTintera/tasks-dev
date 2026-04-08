package eu.tintera.tasks

import androidx.sqlite.SQLiteDriver
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class IosTasksManagerConfiguration(
    val sqLiteDriver: SQLiteDriver? = null,
    val databaseName: String = "",
    val maxConcurrentTasks: Int = 10,
    val executionContextReleaseDebounce: Duration = 1.5.seconds,
    val bgProcessingTaskIdentifier: String? = null,
)