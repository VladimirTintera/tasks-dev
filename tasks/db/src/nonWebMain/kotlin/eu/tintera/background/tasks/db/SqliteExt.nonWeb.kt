package eu.tintera.background.tasks.db

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/** Tady je `androidx.sqlite.execSQL` **synchronní** — suspend přidává až náš wrapper. */
internal actual suspend fun SQLiteConnection.execute(sql: String) = execSQL(sql)
