package eu.tintera.tasks.db

import androidx.room.RoomDatabase
import androidx.sqlite.SQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

internal fun RoomDatabase.Builder<TasksDatabase>.toDatabase(
    driver: SQLiteDriver
): TasksDatabase = this
    .addMigrations(Migration9to10)
    .fallbackToDestructiveMigration(true)
    .setDriver(driver)
    .setQueryCoroutineContext(Dispatchers.IO).build()