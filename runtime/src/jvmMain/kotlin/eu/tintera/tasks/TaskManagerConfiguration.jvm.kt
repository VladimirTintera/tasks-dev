package eu.tintera.tasks

import eu.tintera.guard.ExecutionEnvironment
import java.nio.file.Paths
import kotlin.io.path.absolutePathString
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

actual class TaskManagerConfiguration(
    val databasePath: String,
    actual val databaseName: String = "",
    val maxConcurrentTasks: Int = 10,
    actual val executionContextReleaseDebounce: Duration = 1.5.seconds,
    actual val executionEnvironment: ExecutionEnvironment? = null
) {
    constructor(
        appName: String,
        databaseName: String = "",
        maxConcurrentTasks: Int = 10,
        executionContextReleaseDebounce: Duration = 1.5.seconds
    ) : this(
        databasePath = defaultAppDirectory(appName),
        databaseName = databaseName,
        maxConcurrentTasks = maxConcurrentTasks,
        executionContextReleaseDebounce = executionContextReleaseDebounce
    )

    init {
        require(maxConcurrentTasks > 0) { "maxConcurrentTasks must be > 0" }
        require(!executionContextReleaseDebounce.isNegative()) { "executionContextReleaseDebounce must be >= 0" }
        require(!databasePath.isBlank()) { "databasePath must not be blank" }
    }
}

fun defaultAppDirectory(appName: String): String {
    val os = System.getProperty("os.name").lowercase()
    val userHome = System.getProperty("user.home")

    val path = when {
        os.contains("win") -> {
            System.getenv("APPDATA")?.let { Paths.get(it, appName) }
                ?: Paths.get(userHome, "AppData", "Roaming", appName)
        }

        os.contains("mac") -> {
            Paths.get(userHome, "Library", "Application Support", appName)
        }

        os.contains("nix") || os.contains("nux") || os.contains("aix") -> {
            System.getenv("XDG_DATA_HOME")?.let { Paths.get(it, appName) }
                ?: Paths.get(userHome, ".local", "share", appName)
        }

        else -> Paths.get(userHome, ".$appName")
    }

    return path.absolutePathString()
}