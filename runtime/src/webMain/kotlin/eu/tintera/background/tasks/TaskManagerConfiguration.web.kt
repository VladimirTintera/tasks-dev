package eu.tintera.background.tasks

import androidx.sqlite.SQLiteDriver
import eu.tintera.background.guard.ExecutionEnvironment
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

actual class TaskManagerConfiguration(
    val maxConcurrentTasks: Int = 10,
    actual val executionContextReleaseDebounce: Duration = 1.5.seconds,
    actual val executionEnvironment: ExecutionEnvironment? = null,
    val sqLiteDriver: SQLiteDriver? = null,
    actual val databaseName: String = "",
)