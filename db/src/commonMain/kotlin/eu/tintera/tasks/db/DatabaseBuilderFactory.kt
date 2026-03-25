package eu.tintera.tasks.db

import androidx.room.RoomDatabase

internal interface DatabaseBuilderFactory {
    fun create(name: String): RoomDatabase.Builder<TasksDatabase>
}