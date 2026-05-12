package eu.tintera.tasks.db

import androidx.room3.Room
import kotlinx.coroutines.Dispatchers

internal class WebDatabaseBuilderFactory : DatabaseBuilderFactory {

    override fun create(
        name: String
    ) = Room.databaseBuilder<TasksDatabase>(name).setQueryCoroutineContext(Dispatchers.Default)
}