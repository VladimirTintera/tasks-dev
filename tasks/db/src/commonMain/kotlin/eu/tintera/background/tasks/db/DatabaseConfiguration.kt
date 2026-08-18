package eu.tintera.background.tasks.db

interface DatabaseConfiguration {

    /** Jméno souboru databáze. Prázdné = výchozí `eu.tintera.tasks.db`. */
    val databaseName: String

    /**
     * Adresář, kam soubor umístit. `null` = platformní výchozí (Android `getDatabasePath`,
     * iOS Application Support). Na JVM je povinný — desktopová aplikace si adresář určuje sama.
     *
     * Na webu se ignoruje: tam databáze žije v OPFS, ne v souborovém adresáři.
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
