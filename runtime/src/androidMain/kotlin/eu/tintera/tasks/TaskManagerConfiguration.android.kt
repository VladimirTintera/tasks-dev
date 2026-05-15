package eu.tintera.tasks

import android.content.Context
import androidx.sqlite.SQLiteDriver
import eu.tintera.guard.ExecutionEnvironment
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

actual class TaskManagerConfiguration(
    val context: Context,
    actual val executionContextReleaseDebounce: Duration = 1.5.seconds,
    val compatTransformation: (Map<String, Any?>) -> ByteArray? = { null },
    val sqLiteDriver: SQLiteDriver? = null,
    actual val databaseName: String = "",
    actual val executionEnvironment: ExecutionEnvironment? = null,
)