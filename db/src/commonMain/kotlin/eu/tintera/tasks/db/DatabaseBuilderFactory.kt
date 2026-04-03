package eu.tintera.tasks.db

import androidx.room.RoomDatabase

internal fun interface DatabaseBuilderFactory {
    fun create(name: String): RoomDatabase.Builder<TasksDatabase>
}