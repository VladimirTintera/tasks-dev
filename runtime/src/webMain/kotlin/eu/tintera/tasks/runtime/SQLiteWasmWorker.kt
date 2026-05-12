package eu.tintera.tasks.runtime

import androidx.sqlite.driver.web.WebWorkerSQLiteDriver

internal expect fun createSQLiteWasmWorker(): WebWorkerSQLiteDriver