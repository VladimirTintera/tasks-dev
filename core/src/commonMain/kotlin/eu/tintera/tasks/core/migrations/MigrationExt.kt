package eu.tintera.tasks.core.migrations

import eu.tintera.tasks.migrations.Migration

internal fun List<Migration>.findMigrationPath(
    startVersion: Int,
    targetVersion: Int,
): List<Migration> {
    if (startVersion == targetVersion) return emptyList()
    if (startVersion > targetVersion) {
        error("Downgrade z verze $startVersion na $targetVersion není podporován.")
    }

    // BFS (Breadth-First Search) fronta pro hledání nejkratší cesty.
    // Držíme v ní celé "cesty" (seznamy migrací), které jsme zatím prošli.
    val queue = ArrayDeque<List<Migration>>()

    // Inicializace: Najdeme všechny migrace, které začínají v naší startovací verzi
    this.filter { it.startVersion == startVersion && it.endVersion <= targetVersion }
        .forEach { queue.add(listOf(it)) }

    // Set pro sledování již navštívených verzí, abychom nezacyklili algoritmus
    val visited = mutableSetOf(startVersion)

    while (queue.isNotEmpty()) {
        val currentPath = queue.removeFirst()
        val currentVersion = currentPath.last().endVersion

        // Našli jsme cestu do cíle! (Díky BFS je zaručeně ta nejkratší možná)
        if (currentVersion == targetVersion) {
            return currentPath
        }

        if (visited.add(currentVersion)) {
            // Najdeme všechny další kroky z aktuální verze
            this.filter { it.startVersion == currentVersion && it.endVersion <= targetVersion }
                .sortedByDescending { it.endVersion } // Malá optimalizace: zkoušíme delší skoky dřív
                .forEach { nextMigration ->
                    queue.add(currentPath + nextMigration)
                }
        }
    }

    // Pokud se fronta vyprázdní a my cíl nenašli, cesta neexistuje
    error("Nelze najít migrační cestu z verze $startVersion na verzi $targetVersion. Zkontroluj definované migrace v registraci úkolu.")
}