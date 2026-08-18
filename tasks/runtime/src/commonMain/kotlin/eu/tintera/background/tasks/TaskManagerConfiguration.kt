package eu.tintera.background.tasks

import eu.tintera.background.guard.ExecutionEnvironment
import kotlin.time.Duration

expect class TaskManagerConfiguration {
    val executionEnvironment: ExecutionEnvironment?
    val executionContextReleaseDebounce: Duration
    val databaseName: String

    /** Adresář databáze; `null` = platformní výchozí. Viz `DatabaseConfiguration.databaseDirectory`. */
    val databaseDirectory: String?

    /** Smí Room při rozbité migrační cestě databázi smazat? Výchozí `false`. */
    val allowDestructiveMigration: Boolean
}