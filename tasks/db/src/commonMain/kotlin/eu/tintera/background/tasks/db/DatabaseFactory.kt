package eu.tintera.background.tasks.db

import androidx.sqlite.SQLiteDriver

internal class DatabaseFactory(
    private val builder: DatabaseBuilderFactory,
    private val databaseConfiguration: DatabaseConfiguration,
    private val driver: SQLiteDriver
) {
    fun create(): TasksDatabase = builder.create(
        name = databaseConfiguration.databaseName.ifEmpty { DEFAULT_DATABASE_NAME },
        directory = databaseConfiguration.databaseDirectory,
    ).apply {
        // Only when the application explicitly asks for it — see DatabaseConfiguration.allowDestructiveMigration.
        if (databaseConfiguration.allowDestructiveMigration) fallbackToDestructiveMigration(true)
        setDriver(driver)
    }.build()
}

internal const val DEFAULT_DATABASE_NAME = "eu.tintera.tasks.db"
