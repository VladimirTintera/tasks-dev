package eu.tintera.background.tasks.db

interface DatabaseConfiguration {

    /** Database file name. Empty = the default `eu.tintera.tasks.db`. */
    val databaseName: String

    /**
     * Directory to place the file in. `null` = the platform default (Android `getDatabasePath`,
     * iOS Application Support). Required on JVM — a desktop application picks its own directory.
     *
     * Ignored on web: there the database lives in OPFS, not in a filesystem directory.
     */
    val databaseDirectory: String? get() = null

    /**
     * May Room **delete** the database and create an empty one when a migration path is missing
     * or broken?
     *
     * Defaults to `false` on purpose: the destructive fallback throws away the whole queue of
     * scheduled tasks, and it does so silently — which is worse than a loud crash at startup that
     * a developer will notice. Enable in debug builds at most.
     */
    val allowDestructiveMigration: Boolean get() = false
}
