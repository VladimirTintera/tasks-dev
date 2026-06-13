package eu.tintera.background.tasks.db

import androidx.room3.Room
import androidx.room3.RoomDatabase
import java.io.File

internal class JvmDatabaseBuilderFactory(
    private val config: JvmDatabaseConfiguration
) : DatabaseBuilderFactory {

    override fun create(name: String): RoomDatabase.Builder<TasksDatabase> {

        val directory = File(config.databasePath)
        if (!directory.exists()) directory.mkdirs()
        val dbFilePath = File(config.databasePath, name).absolutePath

        return Room.databaseBuilder<TasksDatabase>(
            name = dbFilePath,
        )
    }
}