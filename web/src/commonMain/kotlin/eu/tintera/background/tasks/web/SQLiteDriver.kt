package eu.tintera.background.tasks.web

import androidx.sqlite.SQLiteDriver

fun sqliteDriver(): SQLiteDriver = createSQLiteWasmWorker()