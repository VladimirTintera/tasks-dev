package eu.tintera.background.tasks.db

import androidx.room3.Room
import kotlinx.coroutines.Dispatchers

internal class WebDatabaseBuilderFactory : DatabaseBuilderFactory {

    // directory is ignored: in the browser the database lives in OPFS/IndexedDB, not a directory.
    override fun create(
        name: String,
        directory: String?,
    ) = Room.databaseBuilder<TasksDatabase>(name).setQueryCoroutineContext(Dispatchers.Default)
}
