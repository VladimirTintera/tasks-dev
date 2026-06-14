package eu.tintera.background.tasks.core.migrations

interface MigratableData {
    val version: Int
    val inputData: ByteArray?
    val outputData: ByteArray?
    val progressData: ByteArray?
}