package eu.tintera.background.tasks.core.migrations

import eu.tintera.background.tasks.migrations.Migration

fun List<Migration>.findMigrationPath(
    startVersion: Int,
    targetVersion: Int,
): List<Migration> {
    if (startVersion == targetVersion) return emptyList()
    if (startVersion > targetVersion) {
        error("Downgrade from version $startVersion to $targetVersion is not supported.")
    }

    // Breadth-first search for the shortest path. The queue holds whole paths (lists of
    // migrations) explored so far.
    val queue = ArrayDeque<List<Migration>>()

    // Seed with every migration starting at the source version.
    this.filter { it.startVersion == startVersion && it.endVersion <= targetVersion }
        .forEach { queue.add(listOf(it)) }

    // Visited versions, so the search cannot loop.
    val visited = mutableSetOf(startVersion)

    while (queue.isNotEmpty()) {
        val currentPath = queue.removeFirst()
        val currentVersion = currentPath.last().endVersion

        // Target reached — breadth-first guarantees this is the shortest path.
        if (currentVersion == targetVersion) {
            return currentPath
        }

        if (visited.add(currentVersion)) {
            // Every next step from the current version.
            this.filter { it.startVersion == currentVersion && it.endVersion <= targetVersion }
                .sortedByDescending { it.endVersion } // Small optimisation: try longer jumps first
                .forEach { nextMigration ->
                    queue.add(currentPath + nextMigration)
                }
        }
    }

    // Queue exhausted without reaching the target — no path exists.
    error("No migration path from version $startVersion to version $targetVersion. Check the migrations declared in the task registration.")
}