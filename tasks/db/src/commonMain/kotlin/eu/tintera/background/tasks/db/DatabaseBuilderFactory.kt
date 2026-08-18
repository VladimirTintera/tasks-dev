package eu.tintera.background.tasks.db

import androidx.room3.RoomDatabase

internal fun interface DatabaseBuilderFactory {
    /**
     * @param name database file name
     * @param directory directory to use; `null` = the platform default
     */
    fun create(name: String, directory: String?): RoomDatabase.Builder<TasksDatabase>
}
