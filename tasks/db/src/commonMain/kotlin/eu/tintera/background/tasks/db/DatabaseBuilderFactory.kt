package eu.tintera.background.tasks.db

import androidx.room3.RoomDatabase

internal fun interface DatabaseBuilderFactory {
    /**
     * @param name jméno souboru databáze
     * @param directory adresář; `null` = platformní výchozí
     */
    fun create(name: String, directory: String?): RoomDatabase.Builder<TasksDatabase>
}
