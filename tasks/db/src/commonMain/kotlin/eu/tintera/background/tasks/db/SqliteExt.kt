package eu.tintera.background.tasks.db

import androidx.sqlite.SQLiteConnection

/**
 * Spustí jeden SQL příkaz bez výsledku.
 *
 * Proč expect/actual a ne `androidx.sqlite.execSQL`: `SQLiteConnection` je v commonMain jen
 * `expect interface` s `close()` a `inTransaction()`. `prepare()` — a tím pádem i `execSQL` —
 * přidávají až actuals, a každý jinak: non-web (android/ios/jvm) synchronně, web `suspend`
 * (SQLite ve WASM workeru je asynchronní). Z commonMain, které pokrývá obojí, proto na `execSQL`
 * nedosáhneme a ruční migrace by se nepřeložila.
 *
 * `suspend` tu nic nestojí — `Migration.migrate` je suspend tak jako tak.
 */
internal expect suspend fun SQLiteConnection.execute(sql: String)
