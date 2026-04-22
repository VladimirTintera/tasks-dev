package eu.tintera.tasks.db

import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

internal class IosDatabaseBuilderFactory : DatabaseBuilderFactory {

    override fun create(name: String): RoomDatabase.Builder<TasksDatabase> {
        val dbFilePath = applicationSupportDirectory() + "/$name"
        return Room.databaseBuilder<TasksDatabase>(
            name = dbFilePath,
        )
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun applicationSupportDirectory(): String {
    val applicationSupportDirectory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSApplicationSupportDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true, // DŮLEŽITÉ: Musí být true, aby se složka vytvořila, pokud neexistuje
        error = null,
    )
    return requireNotNull(applicationSupportDirectory?.path)
}