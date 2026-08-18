package eu.tintera.background.tasks.db

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/** Tady je `androidx.sqlite.execSQL` **suspend** — SQLite ve WASM workeru je asynchronní. */
internal actual suspend fun SQLiteConnection.execute(sql: String) = execSQL(sql)
