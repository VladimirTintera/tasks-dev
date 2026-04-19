package eu.tintera.tasks.db

import androidx.sqlite.SQLiteDriver

internal class DatabaseFactory(
    private val builder: DatabaseBuilderFactory,
    private val databaseConfiguration: DatabaseConfiguration,
    private val driver: SQLiteDriver
) {
    fun create(): TasksDatabase = builder.create(
        databaseConfiguration.databaseName.ifEmpty { "eu.tintera.tasks.db" }
    ).apply {
        addMigrations(Migration9to10)
        fallbackToDestructiveMigration(true)
        setDriver(driver)
    }.build()
}