package eu.tintera.tasks.web

import androidx.sqlite.driver.web.WebWorkerSQLiteDriver
import org.w3c.dom.Worker

internal actual fun createSQLiteWasmWorker() =
    WebWorkerSQLiteDriver(Worker(js("""new URL("sqlite-wasm-worker/worker.js", import.meta.url)""")))