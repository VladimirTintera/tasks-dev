package eu.tintera.tasks.db

import android.content.Context
import androidx.room3.Room
import androidx.room3.RoomDatabase

internal class AndroidDatabaseBuilderFactory(
    private val context: Context
) : DatabaseBuilderFactory {

    override fun create(name: String): RoomDatabase.Builder<TasksDatabase> {
        val ctx = context.applicationContext
        val dbFile = ctx.getDatabasePath(name)
        return Room.databaseBuilder<TasksDatabase>(
            context = ctx,
            name = dbFile.absolutePath
        )
    }
}