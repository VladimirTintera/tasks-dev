package eu.tintera.background.tasks.db

import androidx.room3.Room
import androidx.room3.RoomDatabase
import java.io.File

internal class JvmDatabaseBuilderFactory : DatabaseBuilderFactory {

    override fun create(name: String, directory: String?): RoomDatabase.Builder<TasksDatabase> {
        require(!directory.isNullOrBlank()) { "databaseDirectory must be set on JVM" }

        File(directory).mkdirs()

        return Room.databaseBuilder<TasksDatabase>(
            name = File(directory, name).absolutePath,
        )
    }
}
