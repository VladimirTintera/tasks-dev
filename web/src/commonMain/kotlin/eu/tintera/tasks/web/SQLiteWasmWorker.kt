package eu.tintera.tasks.web

import androidx.sqlite.driver.web.WebWorkerSQLiteDriver

internal expect fun createSQLiteWasmWorker(): WebWorkerSQLiteDriver