package eu.tintera.tasks.web

import androidx.sqlite.SQLiteDriver

fun sqliteDriver(): SQLiteDriver = createSQLiteWasmWorker()