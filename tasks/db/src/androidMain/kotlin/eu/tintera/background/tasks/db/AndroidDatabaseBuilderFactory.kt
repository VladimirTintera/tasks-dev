package eu.tintera.background.tasks.db

import android.content.Context
import androidx.room3.Room
import androidx.room3.RoomDatabase
import java.io.File

internal class AndroidDatabaseBuilderFactory(
    private val context: Context
) : DatabaseBuilderFactory {

    override fun create(name: String, directory: String?): RoomDatabase.Builder<TasksDatabase> {
        val ctx = context.applicationContext
        val dbFile = directory?.let { File(it, name) } ?: ctx.getDatabasePath(name)
        dbFile.parentFile?.mkdirs()
        return Room.databaseBuilder<TasksDatabase>(
            context = ctx,
            name = dbFile.absolutePath
        )
    }
}
