package eu.tintera.tasks.db

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

internal object Migration9to10 : Migration(9, 10) {

    override suspend fun migrate(connection: SQLiteConnection) {
        // 1. Vytvoření pouze nové rodičovské tabulky Task_new
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `Task_new` (`id` TEXT NOT NULL, `identifier` TEXT NOT NULL, `uniqueName` TEXT NOT NULL, `runAttemptCount` INTEGER NOT NULL, `initialDelay` INTEGER NOT NULL, `processTime` INTEGER, `state` TEXT NOT NULL, `inputData` BLOB, `outputData` BLOB, `networkRequired` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `finishedAt` INTEGER, `repeatInterval` INTEGER, `backoffCriteria` BLOB, `progressData` BLOB, `retentionDelay` INTEGER NOT NULL DEFAULT 86400000, `requiresDeviceIdle` INTEGER NOT NULL DEFAULT 0, `version` INTEGER NOT NULL DEFAULT 1, PRIMARY KEY(`id`))
        """.trimIndent()
        )

        // 2. Přesun dat z Task do Task_new
        connection.execSQL(
            """
            INSERT INTO `Task_new` (`id`, `identifier`, `uniqueName`, `runAttemptCount`, `initialDelay`, `processTime`, `state`, `inputData`, `outputData`, `networkRequired`, `createdAt`, `finishedAt`, `repeatInterval`, `backoffCriteria`, `progressData`, `retentionDelay`, `requiresDeviceIdle`, `version`)
            SELECT `id`, `identifier`, `uniqueName`, `runAttemptCount`, `initialDelay`, 
                   CASE WHEN `processTime` IS NOT NULL THEN CAST((julianday(`processTime`) - 2440587.5) * 86400000 AS INTEGER) ELSE NULL END, 
                   `state`, `inputData`, `outputData`, `networkRequired`, 
                   CAST((julianday(`createdAt`) - 2440587.5) * 86400000 AS INTEGER), 
                   CASE WHEN `finishedAt` IS NOT NULL THEN CAST((julianday(`finishedAt`) - 2440587.5) * 86400000 AS INTEGER) ELSE NULL END, 
                   `repeatInterval`, `backoffCriteria`, `progressData`, `retentionDelay`, `requiresDeviceIdle`, `version` 
            FROM `Task`
        """.trimIndent()
        )

        // 3. Smazání STARÉ rodičovské tabulky (dceřiných si nevšímáme, přežijí to)
        connection.execSQL("DROP TABLE `Task`")

        // 4. Přejmenování Task_new na Task (čímž se existující dceřiné tabulky znovu navážou)
        connection.execSQL("ALTER TABLE `Task_new` RENAME TO `Task`")

        // 5. Znovuvytvoření indexů POUZE pro tabulku Task
        // (Indexy pro TaskParentTask přežily, protože jsme tu tabulku nedropnuli)
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_Task_state_processTime` ON `Task` (`state`, `processTime`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_Task_uniqueName` ON `Task` (`uniqueName`)")
    }
}
