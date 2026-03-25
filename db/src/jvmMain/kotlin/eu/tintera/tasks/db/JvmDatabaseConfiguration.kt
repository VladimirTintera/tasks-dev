package eu.tintera.tasks.db

interface JvmDatabaseConfiguration : DatabaseConfiguration {
    val databasePath: String
}