package eu.tintera.background.tasks

import eu.tintera.background.guard.ExecutionEnvironment
import kotlin.time.Duration

expect class TaskManagerConfiguration {
    val executionEnvironment: ExecutionEnvironment?
    val executionContextReleaseDebounce: Duration
    val databaseName: String

    /** Database directory; `null` = the platform default. See `DatabaseConfiguration.databaseDirectory`. */
    val databaseDirectory: String?

    /** May Room delete the database when a migration path is broken? Defaults to `false`. */
    val allowDestructiveMigration: Boolean

    /**
     * How long to wait after the first registry lookup for registrations to settle.
     *
     * Covers the race between the system, which can run a task right after process start, and the
     * application, which registers its handlers only while building its own Koin. Raise it when the
     * application has a slow cold start — typically a background wake-up on a low-end device.
     */
    val registryWarmupTimeout: Duration
}