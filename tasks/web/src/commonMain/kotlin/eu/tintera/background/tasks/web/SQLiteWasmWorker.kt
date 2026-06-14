package eu.tintera.background.tasks.web

import androidx.sqlite.driver.web.WebWorkerSQLiteDriver

internal expect fun createSQLiteWasmWorker(): WebWorkerSQLiteDriver