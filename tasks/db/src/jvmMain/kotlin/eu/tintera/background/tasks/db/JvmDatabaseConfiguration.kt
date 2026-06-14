package eu.tintera.background.tasks.db

interface JvmDatabaseConfiguration : DatabaseConfiguration {
    val databasePath: String
}