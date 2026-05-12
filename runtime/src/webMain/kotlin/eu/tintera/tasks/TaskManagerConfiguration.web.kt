package eu.tintera.tasks

import androidx.sqlite.SQLiteDriver
import eu.tintera.guard.ExecutionEnvironment
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

actual class TaskManagerConfiguration(
    val maxConcurrentTasks: Int = 10,
    val executionContextReleaseDebounce: Duration = 1.5.seconds,
    val executionEnvironment: ExecutionEnvironment? = null,
    val sqLiteDriver: SQLiteDriver? = null,
    val databaseName: String = "",
)