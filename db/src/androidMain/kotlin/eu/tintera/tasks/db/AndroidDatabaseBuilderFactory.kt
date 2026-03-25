package eu.tintera.tasks.db

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

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