package eu.tintera.tasks.db

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.AndroidSQLiteDriver

internal fun getDatabase(context: Context): TasksDatabase {
    val ctx = context.applicationContext
    val dbFile = ctx.getDatabasePath(databaseFile)
    return Room.databaseBuilder<TasksDatabase>(
        context = ctx,
        name = dbFile.absolutePath
    ).toDatabase(AndroidSQLiteDriver())
}