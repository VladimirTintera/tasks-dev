package eu.tintera.background.tasks

import android.content.Context
import androidx.sqlite.SQLiteDriver
import eu.tintera.background.guard.ExecutionEnvironment
import eu.tintera.background.tasks.runtime.DEFAULT_WARMUP_TIMEOUT
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

actual class TaskManagerConfiguration(
    val context: Context,
    actual val executionContextReleaseDebounce: Duration = 1.5.seconds,
    val compatTransformation: (Map<String, Any?>) -> ByteArray? = { null },
    val sqLiteDriver: SQLiteDriver? = null,
    actual val databaseName: String = "",
    actual val databaseDirectory: String? = null,
    actual val allowDestructiveMigration: Boolean = false,
    actual val registryWarmupTimeout: Duration = DEFAULT_WARMUP_TIMEOUT,
    actual val executionEnvironment: ExecutionEnvironment? = null,
)