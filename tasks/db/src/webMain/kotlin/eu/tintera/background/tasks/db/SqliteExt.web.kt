package eu.tintera.background.tasks.db

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

// web: androidx.sqlite.execSQL je tu suspend (SQLite ve WASM workeru je asynchronní).
internal actual suspend fun SQLiteConnection.execute(sql: String) = execSQL(sql)
