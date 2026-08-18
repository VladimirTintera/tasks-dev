package eu.tintera.background.tasks.db

interface DatabaseConfiguration {

    /** Jméno souboru databáze. Prázdné = výchozí `eu.tintera.tasks.db`. */
    val databaseName: String

    /**
     * Adresář, kam soubor umístit. `null` = platformní výchozí (Android `getDatabasePath`,
     * iOS Application Support, JVM adresář aplikace).
     *
     * Existuje kvůli přebírání databáze z jiného umístění — typicky když aplikace migruje
     * z předchozího enginu, který si soubor držel jinde.
     */
    val databaseDirectory: String? get() = null

    /**
     * Smí Room při chybějící nebo rozbité migrační cestě databázi **smazat** a založit prázdnou?
     *
     * Výchozí `false` záměrně: destruktivní fallback zahodí celou frontu naplánovaných tasků, a to
     * beze slova — což je horší než hlasitý pád při startu, který si vývojář všimne. Zapínat leda
     * v debug buildech.
     */
    val allowDestructiveMigration: Boolean get() = false
}
