package eu.tintera.tasks

import eu.tintera.guard.ExecutionEnvironment
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class JvmTasksManagerConfiguration(
    val databasePath: String,
    val databaseName: String = "",
    val maxConcurrentTasks: Int = 10,
    val executionContextReleaseDebounce: Duration = 1.5.seconds,
    val executionEnvironment: ExecutionEnvironment? = null
) {

    init {
        require(maxConcurrentTasks > 0) { "maxConcurrentTasks must be > 0" }
        require(!executionContextReleaseDebounce.isNegative()) { "executionContextReleaseDebounce must be >= 0" }
        require(!databasePath.isBlank()) { "databasePath must not be blank" }
    }
}

fun defaultJvmTasksManagerConfiguration(
    appName: String,
    databaseName: String = "",
    maxConcurrentTasks: Int = 10,
    executionContextReleaseDebounce: Duration = 1.5.seconds
) = JvmTasksManagerConfiguration(
    databasePath = defaultAppDirectory(appName),
    databaseName = databaseName,
    maxConcurrentTasks = maxConcurrentTasks,
    executionContextReleaseDebounce = executionContextReleaseDebounce
)

fun defaultAppDirectory(appName: String): String {
    val os = System.getProperty("os.name").lowercase()
    val userHome = System.getProperty("user.home")

    return when {
        os.contains("win") -> {
            // Windows: Pokusíme se vzít APPDATA z proměnné prostředí, jinak složíme cestu ručně
            System.getenv("APPDATA")?.let { "$it\\$appName" }
                ?: "$userHome\\AppData\\Roaming\\$appName"
        }
        os.contains("mac") -> {
            // macOS: Klasický Application Support
            "$userHome/Library/Application Support/$appName"
        }
        os.contains("nix") || os.contains("nux") || os.contains("aix") -> {
            // Linux: Preferujeme XDG_DATA_HOME, jinak použijeme výchozí ~/.local/share
            System.getenv("XDG_DATA_HOME")?.let { "$it/$appName" }
                ?: "$userHome/.local/share/$appName"
        }
        else -> {
            // Bezpečný fallback pro neznámé systémy
            "$userHome/.$appName"
        }
    }
}