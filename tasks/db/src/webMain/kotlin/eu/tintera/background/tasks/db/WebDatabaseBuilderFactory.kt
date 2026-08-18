package eu.tintera.background.tasks.db

import androidx.room3.Room
import kotlinx.coroutines.Dispatchers

internal class WebDatabaseBuilderFactory : DatabaseBuilderFactory {

    // directory se ignoruje: v prohlížeči je databáze v OPFS/IndexedDB, ne v adresáři.
    override fun create(
        name: String,
        directory: String?,
    ) = Room.databaseBuilder<TasksDatabase>(name).setQueryCoroutineContext(Dispatchers.Default)
}
