package eu.tintera.background.tasks.db

import androidx.sqlite.SQLiteConnection

/**
 * Spustí jeden SQL příkaz bez výsledku.
 *
 * Proč vlastní wrapper a ne rovnou `androidx.sqlite.execSQL`: `SQLiteConnection` je v commonMain jen
 * `expect interface` s `close()` a `inTransaction()`. `prepare()` — a tím pádem i `execSQL` —
 * přidávají až actuals, a **každý jinak**:
 *
 * ```
 * // nonWebMain          public fun SQLiteConnection.execSQL(sql: String)
 * // webMain     public suspend fun SQLiteConnection.execSQL(sql: String)
 * ```
 *
 * Jsou to dvě různé deklarace lišící se `suspend` a ani jedna není v commonMain, takže z modulu,
 * který cílí na obě větve, na ně nedosáhneme — ruční migrace psaná v commonMain se nepřeloží.
 * Obě `actual` implementace proto vypadají stejně (`= execSQL(sql)`), ale volají jinou funkci.
 *
 * `suspend` na naší straně nic nestojí — `Migration.migrate` je suspend tak jako tak.
 */
internal expect suspend fun SQLiteConnection.execute(sql: String)
