package eu.tintera.background.tasks

import eu.tintera.background.guard.ExecutionEnvironment
import kotlin.time.Duration

expect class TaskManagerConfiguration {
    val executionEnvironment: ExecutionEnvironment?
    val executionContextReleaseDebounce: Duration
    val databaseName: String

    /** Adresář databáze; `null` = platformní výchozí. Viz `DatabaseConfiguration.databaseDirectory`. */
    val databaseDirectory: String?

    /** Smí Room při rozbité migrační cestě databázi smazat? Výchozí `false`. */
    val allowDestructiveMigration: Boolean

    /**
     * Jak dlouho po prvním dotazu na registr čekat, než se registrace usadí.
     *
     * Kryje závod mezi systémem, který umí spustit task hned po startu procesu, a aplikací, která
     * své handlery registruje až při stavbě vlastního Koinu. Zvýšit, pokud má aplikace pomalý
     * studený start (typicky probuzení na pozadí na slabém zařízení).
     */
    val registryWarmupTimeout: Duration
}