package eu.tintera.tasks.db

import androidx.room.Room
import androidx.sqlite.driver.NativeSQLiteDriver

internal fun getDatabase(): TasksDatabase {
    val dbFilePath = documentDirectory() + "/${databaseFile}"
    return Room.databaseBuilder<TasksDatabase>(
        name = dbFilePath,
    ).toDatabase(NativeSQLiteDriver())
}