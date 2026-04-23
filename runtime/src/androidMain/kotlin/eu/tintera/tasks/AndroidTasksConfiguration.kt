package eu.tintera.tasks

import android.content.Context
import androidx.sqlite.SQLiteDriver
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class AndroidTasksConfiguration(
    val context: Context,
    val executionContextReleaseDebounce: Duration = 1.5.seconds,
    val compatTransformation: (Map<String, Any?>) -> ByteArray? = { null },
    val sqLiteDriver: SQLiteDriver? = null,
    val databaseName: String = ""
)