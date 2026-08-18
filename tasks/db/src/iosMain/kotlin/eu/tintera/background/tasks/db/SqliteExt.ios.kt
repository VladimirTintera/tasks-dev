package eu.tintera.background.tasks.db

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

// non-web: androidx.sqlite.execSQL je tu synchronní.
internal actual suspend fun SQLiteConnection.execute(sql: String) = execSQL(sql)
